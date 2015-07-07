name := "Scalabha"

version := "0.2.6"

organization := "opennlp"

scalaVersion := "2.11.6"

crossPaths := false

retrieveManaged := true

resolvers ++= Seq(
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
  "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/",
  "opennlp sourceforge repo" at "http://opennlp.sourceforge.net/maven2",
  "repo.codahale.com" at "http://repo.codahale.com"
)

libraryDependencies ++= Seq(
  "org.apache.opennlp" % "opennlp-tools" % "1.5.3",
  "org.clapper" % "argot_2.11" % "1.0.3",
//  "net.sf.opencsv" % "opencsv" % "2.3",
//  "no.arktekk" % "anti-xml_2.11" % "0.6.0",
  "com.gilt" % "jerkson_2.11" % "0.6.7",
  "org.scalanlp" %% "breeze" % "0.11.2",
  "org.scalanlp" %% "breeze-natives" % "0.11.2",
  "org.apache.commons" % "commons-lang3" % "3.4",
  "commons-logging" % "commons-logging" % "1.2",
  "org.apache.logging.log4j" % "log4j-core" % "2.2",
  "org.apache.logging.log4j" % "log4j-api" % "2.2",
  "org.scalatest" % "scalatest_2.11" % "3.0.0-SNAP4",
  "junit" % "junit" % "4.12",
  //"org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4",
  "com.novocode" % "junit-interface" % "0.11") //switch to ScalaTest at some point...

