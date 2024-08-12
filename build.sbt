name := "spark-sql-dmn"
version := "1.0"
scalaVersion := "2.12.15"

// Library dependencies
libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "3.5.0" % "provided",
  "org.apache.spark" %% "spark-sql" % "3.5.0" % "provided",
  "org.scalatest" %% "scalatest" % "3.2.18" % "test",
  "org.camunda.bpm.dmn" % "camunda-engine-dmn" % "7.19.0",
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  "org.scala-lang" % "scala-compiler" % scalaVersion.value,
  "org.scala-lang.modules" %% "scala-collection-compat" % "2.12.0",

)

libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always



// Resolver for Spark packages
resolvers += "Spark Packages Repo" at "https://repos.spark-packages.org/"
resolvers += "Maven Central" at "https://repo.maven.apache.org/maven2/"

//// Optional: Add logging dependencies
//libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"

// Optional: Merge strategy to handle conflicts
assembly / assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}



