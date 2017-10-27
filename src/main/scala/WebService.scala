
import java.util.concurrent.TimeUnit
import java.util.{Map => JMap}

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.{entity, _}
import akka.stream.ActorMaterializer
import akka.util.ByteString
import com.typesafe.config.ConfigFactory

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContextExecutor

object WebServer {
    
    val metricRegistry = new com.codahale.metrics.MetricRegistry()
    
    val requestMeter = metricRegistry.meter("echo.server.requestCount")
    
    val startTime = System.currentTimeMillis()
    
    implicit val mapMarshaller: ToEntityMarshaller[Map[String, Any]] = Marshaller.opaque { map =>
        HttpEntity(ContentType(MediaTypes.`application/json`), map.toString)
    }
    
    def main(args: Array[String]) {
        implicit val system: ActorSystem = ActorSystem("echo_server")
        implicit val materializer: ActorMaterializer = ActorMaterializer()
        // needed for the future flatMap/onComplete in the end
        implicit val executionContext: ExecutionContextExecutor = system.dispatcher
        
        val route =
            pathPrefix("echo" /  (IntNumber ?)) { (code) =>
                val statusCode = code.getOrElse(200)
                requestMeter.mark()
                get {
                    complete(HttpResponse(status = statusCode))
                } ~
                        post {
                            entity(as[ByteString]) { input =>
                                complete(HttpResponse(status = statusCode, entity = HttpEntity(input)))
                            } ~
                                    put {
                                        entity(as[ByteString]) { input =>
                                            complete(HttpResponse(status = statusCode, entity = HttpEntity(input)))
                                        } ~
                                                delete {
                                                    entity(as[ByteString]) { input =>
                                                        complete(HttpResponse(status = statusCode, entity = HttpEntity(input)))
                                                    }
                                                }
                                    }
                        }
            } ~
                pathPrefix("status") {
                    get {
                        val runningTime = System.currentTimeMillis() - startTime
                        val requestCount = metricRegistry.getMeters().get("echo.server.requestCount").getCount
                        complete(s"running for ${TimeUnit.MILLISECONDS.toSeconds(runningTime)} seconds. request count = $requestCount")
                    }
                }
        
        
        val port = ConfigFactory.load().getInt("server.port")
        val interface = ConfigFactory.load().getString("server.interface")
        
        val bindingFuture = Http().bindAndHandle(route, interface, port)
        
        bindingFuture.failed.foreach { ex =>
            println(s"Failed to bind to $interface:$port!")
        }
        
        println(s"Server online at http://$interface:$port")
        
    }
    
}