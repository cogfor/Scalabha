package opennlp.scalabha.tag.hmm

import opennlp.scalabha.util.CollectionUtil._
import opennlp.scalabha.util.CollectionUtils._
import breeze.stats.distributions.RandBasis
import breeze.stats.distributions.Rand
import opennlp.scalabha.tag._
import opennlp.scalabha.tag.support._

/**
 * Factory for training an HmmTagger from unlabeled data.
 *
 * @tparam Sym	visible symbols in the sequences
 * @tparam Tag	tags applied to symbols
 */
abstract class TypesupervisedHmmTaggerTrainer[Sym, Tag](
  val supervisedHmmTaggerTrainer: SupervisedHmmTaggerTrainer[Sym, Tag])
  //  transitionCountsTransformer: TransitionCountsTransformer[Tag] = new TransitionCountsTransformer[Tag](AddLambdaSmoothingCondCountsTransformer(1)),
  //  emissionCountsTransformer: EmissionCountsTransformer[Tag, Sym] = new EmissionCountsTransformer[Tag, Sym](AddLambdaSmoothingCondCountsTransformer(1)),
  //  hmmTaggerFactory: HmmTaggerFactory[Sym, Tag] = new HardTagDictConstraintHmmTaggerFactory(tagDict.opt))
  extends TypesupervisedTaggerTrainer[Sym, Tag] {

  /**
   * Train an HMM from unlabeled sequences.
   *
   * NOTE: This is the main entry point for all type-supervised tagger
   * trainers. All other `train` methods should be final and flow through
   * here.
   *
   * @param rawSequences			unlabeled sequences to be used as unsupervised training data
   * @param initialHmm				an initial HMM tagger to be used to bootstrap a new tagger
   * @param tagDict					a mapping from symbols to their possible tags
   * @param priorTransitionCounts	transition prior counts (pseudocounts)
   * @param priorEmissionCounts		emission prior counts (pseudocounts)
   * @return						a trained tagger
   */
  def trainFromInitialHmm(
    rawSequences: Iterable[IndexedSeq[Sym]],
    initialHmm: HmmTagger[Sym, Tag],
    tagDict: TagDict[Sym, Tag],
    priorTransitionCounts: Option[Tag] => Option[Tag] => Double,
    priorEmissionCounts: Option[Tag] => Option[Sym] => Double): HmmTagger[Sym, Tag]

  /////////////////////////////////
  // Convenience training methods
  //   - All delegate to trainFromInitialHmm (above)
  /////////////////////////////////

  /**
   * @inheritdoc
   *
   * The initial HMM is has UNIFORM distributions and NO priors are assumed.
   */
  final override def train(
    rawSequences: Iterable[IndexedSeq[Sym]],
    tagDict: TagDict[Sym, Tag]): Tagger[Sym, Tag] = {

    trainWithPriors(
      rawSequences,
      Function.const(Function.const(0)), Function.const(Function.const(0)),
      Function.const(Function.const(0)), Function.const(Function.const(0)),
      tagDict)
  }

  /**
   * @inheritdoc
   *
   * The initial HMM is has distributions based on the tagged data and
   * counts from the tagged data are used as priors.
   */
  final override def trainWithSomeGoldLabeled(
    rawSequences: Iterable[IndexedSeq[Sym]],
    goldTaggedSequences: Iterable[IndexedSeq[(Sym, Tag)]],
    tagDict: TagDict[Sym, Tag]): Tagger[Sym, Tag] = {

    val (transitionPriorCounts, emissionPriorCounts) = HmmUtils.getCountsFromTagged(goldTaggedSequences)
    trainWithPriors(
      rawSequences,
      transitionPriorCounts, emissionPriorCounts,
      CondFreqCounts(transitionPriorCounts).toDoubles.toMap, CondFreqCounts(emissionPriorCounts).toDoubles.toMap,
      tagDict)
  }

  /**
   * @inheritdoc
   *
   * The initial HMM is has distributions based on the tagged data and
   * NO prior counts are assumed.
   */
  final override def trainWithSomeNoisyLabeled(
    rawSequences: Iterable[IndexedSeq[Sym]],
    noisyTaggedSequences: Iterable[IndexedSeq[(Sym, Tag)]],
    tagDict: TagDict[Sym, Tag]): Tagger[Sym, Tag] = {

    val (transitionNoisyCounts, emissionNoisyCounts) = HmmUtils.getCountsFromTagged(noisyTaggedSequences)
    trainWithPriors(
      rawSequences,
      transitionNoisyCounts, emissionNoisyCounts,
      Function.const(Function.const(0)), Function.const(Function.const(0)),
      tagDict)
  }

  /**
   * Train a Tagger from a combination of unlabeled data and NOISY labeled data.
   * The labeled data is NOT used for prior counts, NOR is it iterated over
   * like the raw data.  The tagged data could even be EMPTY.
   *
   * A starting point is initialized (initial tagging) by running a MLE-trained
   * HMM on add-one-smoothed counts from the initial counts, strictly
   * constrained by the tag dictionary.  If a more sophisticated procedure is
   * desired for building the initial HMM, then `trainFromInitialHmm` can be
   * called directly.
   *
   * @param rawSequences			unlabeled sequences to be used as unsupervised training data
   * @param initialTransitionCounts	transition counts used to construct the initial HMM
   * @param initialEmissionCounts	emission counts used to construct the initial HMM
   * @param priorTransitionCounts	transition prior counts (pseudocounts)
   * @param priorEmissionCounts		emission prior counts (pseudocounts)
   * @param tagDict					a mapping from symbols to their possible tags
   * @return						a trained Tagger
   */
  final def trainWithPriors(
    rawSequences: Iterable[IndexedSeq[Sym]],
    initialTransitionCounts: Option[Tag] => Option[Tag] => Int, initialEmissionCounts: Option[Tag] => Option[Sym] => Int,
    priorTransitionCounts: Option[Tag] => Option[Tag] => Double, priorEmissionCounts: Option[Tag] => Option[Sym] => Double,
    tagDict: TagDict[Sym, Tag]): Tagger[Sym, Tag] = {

    val transitionCounts = HmmUtils.makeTransitionCounts(tagDict.opt, initialTransitionCounts.andThen(_.andThen(_.toDouble)))
    val emissionCounts = HmmUtils.makeEmissionCounts(tagDict.opt, initialEmissionCounts.andThen(_.andThen(_.toDouble)))

    val initialHmm = supervisedHmmTaggerTrainer.makeTagger(transitionCounts, emissionCounts)

    trainFromInitialHmm(rawSequences, initialHmm, tagDict, priorTransitionCounts, priorEmissionCounts)
  }

  /**
   * Train an HMM from unlabeled sequences.  NO prior counts are assumed.
   *
   * @param rawSequences			unlabeled sequences to be used as unsupervised training data
   * @param initialHmm				an initial HMM tagger to be used to bootstrap a new tagger
   * @param tagDict					a mapping from symbols to their possible tags
   * @return						a trained tagger
   */
  final def trainFromInitialHmm(
    rawSequences: Iterable[IndexedSeq[Sym]],
    initialHmm: HmmTagger[Sym, Tag],
    tagDict: TagDict[Sym, Tag]): HmmTagger[Sym, Tag] = {

    trainFromInitialHmm(
      rawSequences,
      initialHmm,
      tagDict,
      Function.const(Function.const(0)), Function.const(Function.const(0)))
  }

}
