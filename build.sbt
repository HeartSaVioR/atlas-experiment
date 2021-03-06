
name := "atlas-experiment"

version := "0.1"

scalaVersion := "2.12.7"

val atlasVersion = "1.1.0"
val log4jVersion = "1.2.16"
val jerseyVersion = "1.19"

libraryDependencies += "org.apache.atlas" % "atlas-client-v2" % atlasVersion
libraryDependencies += "org.apache.atlas" % "atlas-notification" % atlasVersion
libraryDependencies += "org.apache.atlas" % "atlas-intg" % atlasVersion
libraryDependencies += "com.sun.jersey" % "jersey-json" % jerseyVersion
libraryDependencies += "log4j" % "log4j" % log4jVersion

assemblyMergeStrategy in assembly := {
  case PathList("javax", "servlet", xs @ _*) => MergeStrategy.last
  case PathList("javax", "activation", xs @ _*) => MergeStrategy.last
  case PathList("org", "apache", xs @ _*) => MergeStrategy.last
  case PathList("com", "google", xs @ _*) => MergeStrategy.last
  case PathList("com", "esotericsoftware", xs @ _*) => MergeStrategy.last
  case PathList("com", "codahale", xs @ _*) => MergeStrategy.last
  case PathList("com", "yammer", xs @ _*) => MergeStrategy.last
  // start -- Spark 3.0.0-SNAPSHOT...
  case PathList("org", "aopalliance", xs @ _*) => MergeStrategy.last
  case PathList("javax", "inject", xs @ _*) => MergeStrategy.last
  // end -- Spark 3.0.0-SNAPSHOT...
  case "about.html" => MergeStrategy.rename
  case "META-INF/ECLIPSEF.RSA" => MergeStrategy.last
  case "META-INF/mailcap" => MergeStrategy.last
  case "META-INF/mimetypes.default" => MergeStrategy.last
  case "META-INF/jersey-module-version" => MergeStrategy.last
  case "plugin.properties" => MergeStrategy.last
  case "log4j.properties" => MergeStrategy.last
  // below is due to Apache Arrow...
  case "git.properties" => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

target in assembly := file("build")

/* including scala bloats your assembly jar unnecessarily, and may interfere with
   spark runtime */
assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false)

assemblyJarName in assembly := s"${name.value}.jar"
