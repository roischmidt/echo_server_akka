name := "echo_server_akka"

version := "0.1"

scalaVersion := "2.12.4"


libraryDependencies ++= {
    val akkaVersion = "2.5.4"
    val akkaHttpVersion = "10.0.10"
    
    Seq(
        "com.typesafe.akka" %% "akka-http"   % akkaHttpVersion,
        "com.typesafe.akka" %% "akka-actor"  % akkaVersion,
        "com.typesafe.akka" %% "akka-stream" % akkaVersion,
        "nl.grons" % "metrics-scala_2.12" % "3.5.9"
    )
}