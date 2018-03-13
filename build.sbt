lazy val root = Project("elastic4s", file("."))
  .settings(
    publish := {},
    publishArtifact := false,
    name := "elastic4s",
    mappings in (Compile, packageSrc) ++= {
      val base  = (Compile / sourceManaged).value
      val files = (Compile / managedSources).value
      files.map { f =>
        (f, f.relativeTo(base).get.getPath)
      }
    }
  )
  .aggregate(
    core,
    tcp,
    http,
    akka,
    embedded,
    tests,
    testkit,
    circe,
    jackson,
    json4s,
    playjson,
    sprayjson,
    aws,
    sttp,
    httpstreams,
    xpacksecurity
  )

lazy val core = Project("elastic4s-core", file("elastic4s-core"))
  .settings(
    name := "elastic4s-core",
    libraryDependencies ++= Seq(
      "joda-time"                    % "joda-time"             % "2.9.9",
      "com.fasterxml.jackson.core"   % "jackson-core"          % JacksonVersion,
      "com.fasterxml.jackson.core"   % "jackson-databind"      % JacksonVersion,
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % JacksonVersion
    )
  )

lazy val tcp = Project("elastic4s-tcp", file("elastic4s-tcp"))
  .settings(
    name := "elastic4s-tcp",
    libraryDependencies ++= Seq(
      "org.elasticsearch"          % "elasticsearch"           % ElasticsearchVersion,
      "org.locationtech.spatial4j" % "spatial4j"               % "0.6",
      "com.vividsolutions"         % "jts"                     % "1.13",
      "io.netty"                   % "netty-all"               % "4.1.16.Final",
      "org.apache.lucene"          % "lucene-core"             % LuceneVersion,
      "org.apache.lucene"          % "lucene-analyzers-common" % LuceneVersion,
      "org.apache.lucene"          % "lucene-backward-codecs"  % LuceneVersion,
      "org.apache.lucene"          % "lucene-grouping"         % LuceneVersion,
      "org.apache.lucene"          % "lucene-highlighter"      % LuceneVersion,
      "org.apache.lucene"          % "lucene-join"             % LuceneVersion,
      "org.apache.lucene"          % "lucene-memory"           % LuceneVersion,
      "org.apache.lucene"          % "lucene-misc"             % LuceneVersion,
      "org.apache.lucene"          % "lucene-queries"          % LuceneVersion,
      "org.apache.lucene"          % "lucene-queryparser"      % LuceneVersion,
      "org.apache.lucene"          % "lucene-sandbox"          % LuceneVersion,
      "org.apache.lucene"          % "lucene-spatial"          % LuceneVersion,
      "org.apache.lucene"          % "lucene-spatial-extras"   % LuceneVersion,
      "org.apache.lucene"          % "lucene-spatial3d"        % LuceneVersion,
      "org.apache.lucene"          % "lucene-suggest"          % LuceneVersion,
      "org.elasticsearch.client"   % "transport"               % ElasticsearchVersion,
      "org.apache.lucene"          % "lucene-join"             % LuceneVersion,
      "org.apache.logging.log4j"   % "log4j-api"               % Log4jVersion,
      "org.apache.logging.log4j"   % "log4j-core"              % Log4jVersion,
      "org.apache.logging.log4j"   % "log4j-1.2-api"           % Log4jVersion,
      "org.apache.logging.log4j"   % "log4j-slf4j-impl"        % Log4jVersion,
      "com.carrotsearch"           % "hppc"                    % "0.7.1",
      "joda-time"                  % "joda-time"               % "2.9.9",
      "com.fasterxml.jackson.core" % "jackson-core"            % JacksonVersion,
      "com.tdunning"               % "t-digest"                % "3.1"
    )
  )
  .dependsOn(core)

lazy val http = Project("elastic4s-http", file("elastic4s-http"))
  .settings(
    name := "elastic4s-http",
    libraryDependencies ++= Seq(
      "org.elasticsearch.client"     % "elasticsearch-rest-client" % ElasticsearchVersion,
      "org.apache.logging.log4j"     % "log4j-api"                 % Log4jVersion % "test",
      "com.fasterxml.jackson.core"   % "jackson-core"              % JacksonVersion,
      "com.fasterxml.jackson.core"   % "jackson-databind"          % JacksonVersion,
      "com.fasterxml.jackson.module" %% "jackson-module-scala"     % JacksonVersion exclude ("org.scala-lang", "scala-library")
    )
  )
  .dependsOn(core)

lazy val akka = Project("elastic4s-akka-http", file("elastic4s-akka-http"))
  .settings(
    name := "elastic4s-akka-http",
    libraryDependencies ++= Seq(
      "com.typesafe.akka"           %% "akka-http"                 % "10.1.0-RC2",
      "com.typesafe.akka"           %% "akka-stream"               % "2.5.10",
      "org.elasticsearch.client"     % "elasticsearch-rest-client" % ElasticsearchVersion,
      "org.apache.logging.log4j"     % "log4j-api"                 % Log4jVersion % "test",
      "com.fasterxml.jackson.core"   % "jackson-core"              % JacksonVersion,
      "com.fasterxml.jackson.core"   % "jackson-databind"          % JacksonVersion,
      "com.fasterxml.jackson.module" %% "jackson-module-scala"     % JacksonVersion exclude ("org.scala-lang", "scala-library")
    )
  )
  .dependsOn(http)

lazy val xpacksecurity = Project("elastic4s-xpack-security", file("elastic4s-xpack-security"))
  .settings(
    name := "elastic4s-xpack-security",
    resolvers += "elastic" at "https://artifacts.elastic.co/maven",
    libraryDependencies += "org.elasticsearch.client" % "x-pack-transport" % ElasticsearchVersion
  )
  .dependsOn(tcp)

lazy val embedded = Project("elastic4s-embedded", file("elastic4s-embedded"))
  .settings(
    name := "elastic4s-embedded",
    libraryDependencies ++= Seq(
      "org.elasticsearch"                % "elasticsearch"            % ElasticsearchVersion,
      "com.fasterxml.jackson.dataformat" % "jackson-dataformat-smile" % JacksonVersion,
      "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor"  % JacksonVersion
    )
  )
  .dependsOn(tcp, http)

lazy val testkit = Project("elastic4s-testkit", file("elastic4s-testkit"))
  .settings(
    name := "elastic4s-testkit",
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % ScalatestVersion
    )
  )
  .dependsOn(core, embedded, http, akka, sttp)

lazy val httpstreams = Project("elastic4s-http-streams", file("elastic4s-http-streams"))
  .settings(
    name := "elastic4s-http-streams",
    libraryDependencies += "com.typesafe.akka"   %% "akka-actor"          % AkkaVersion,
    libraryDependencies += "org.reactivestreams" % "reactive-streams"     % ReactiveStreamsVersion,
    libraryDependencies += "org.reactivestreams" % "reactive-streams-tck" % ReactiveStreamsVersion % "test"
  )
  .dependsOn(http, testkit % "test", jackson % "test")

lazy val jackson = Project("elastic4s-jackson", file("elastic4s-jackson"))
  .settings(
    name := "elastic4s-jackson",
    libraryDependencies += "com.fasterxml.jackson.core"     % "jackson-core"          % JacksonVersion,
    libraryDependencies += "com.fasterxml.jackson.core"     % "jackson-databind"      % JacksonVersion,
    libraryDependencies += "com.fasterxml.jackson.module"   %% "jackson-module-scala" % JacksonVersion exclude ("org.scala-lang", "scala-library"),
    libraryDependencies += "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % JacksonVersion
  )
  .dependsOn(core)

lazy val circe = Project("elastic4s-circe", file("elastic4s-circe"))
  .settings(
    name := "elastic4s-circe",
    libraryDependencies += "io.circe" %% "circe-core"    % CirceVersion,
    libraryDependencies += "io.circe" %% "circe-generic" % CirceVersion,
    libraryDependencies += "io.circe" %% "circe-parser"  % CirceVersion
  )
  .dependsOn(core)

lazy val json4s = Project("elastic4s-json4s", file("elastic4s-json4s"))
  .settings(
    name := "elastic4s-json4s",
    libraryDependencies += "org.json4s" %% "json4s-core"    % Json4sVersion,
    libraryDependencies += "org.json4s" %% "json4s-jackson" % Json4sVersion
  )
  .dependsOn(core)

lazy val playjson = Project("elastic4s-play-json", file("elastic4s-play-json"))
  .settings(
    name := "elastic4s-play-json",
    libraryDependencies += "com.typesafe.play" %% "play-json" % PlayJsonVersion
  )
  .dependsOn(core)

lazy val sprayjson = Project("elastic4s-spray-json", file("elastic4s-spray-json"))
  .settings(
    name := "elastic4s-spray-json",
    libraryDependencies += "io.spray" %% "spray-json" % SprayJsonVersion
  )
  .dependsOn(core)

lazy val sttp = Project("elastic4s-sttp", file("elastic4s-sttp"))
  .settings(
    name := "elastic4s-sttp",
    libraryDependencies += "com.softwaremill.sttp" %% "core"                             % "1.1.3",
    libraryDependencies += "com.softwaremill.sttp" %% "async-http-client-backend-future" % "1.1.3"
  )
  .dependsOn(core, http)

lazy val aws = Project("elastic4s-aws", file("elastic4s-aws"))
  .settings(
    name := "elastic4s-aws",
    libraryDependencies += "com.amazonaws" % "aws-java-sdk-core" % AWSJavaSdkVersion
  )
  .dependsOn(core, http)

lazy val tests = Project("elastic4s-tests", file("elastic4s-tests"))
  .settings(
    name := "elastic4s-tests",
    libraryDependencies ++= Seq(
      "commons-io"                   % "commons-io"            % CommonsIoVersion % "test",
      "org.mockito"                  % "mockito-all"           % MockitoVersion   % "test",
      "com.fasterxml.jackson.core"   % "jackson-core"          % JacksonVersion   % "test",
      "com.fasterxml.jackson.core"   % "jackson-databind"      % JacksonVersion   % "test",
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % JacksonVersion   % "test" exclude ("org.scala-lang", "scala-library"),
      "org.apache.logging.log4j"     % "log4j-api"             % "2.8.2"          % "test",
      "org.apache.logging.log4j"     % "log4j-slf4j-impl"      % "2.8.2"          % "test",
      "org.apache.logging.log4j"     % "log4j-core"            % "2.8.2"          % "test"
    ),
    Test / fork := true,
    Test / parallelExecution := false,
    Test / testForkedParallel := false
  )
  .dependsOn(tcp, http, akka, jackson, circe, aws, testkit % "test")

lazy val noPublishSettings = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)
