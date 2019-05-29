import SupervisorDownloadImage.DownloadImages
import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import spray.json.DefaultJsonProtocol

case class Images(urls: List[String])

trait JsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val imagesFormat = jsonFormat1(Images)
}

trait RestApi extends Directives with JsonSupport {
  val config = ConfigFactory.load()
  implicit val system = ActorSystem("robo-voice-system", config.getConfig("robo-voice-system").withFallback(config))
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  val ping =
    path("ping") {
      get {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "pong"))
      }
    }

  val images =
    path("v1" / "images" / "upload") {
      post {
        entity(as[Images]) { images =>
          system.actorOf(Props[SupervisorDownloadImage]) ! DownloadImages(images.urls.toSet)
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "Images"))
        }
      }
    } ~
      path("v2" / "images" / "upload") {
        post {
          entity(as[Images]) { images =>
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "Akka Stream Implementation"))
          }
        }
      }

  val route = ping ~ images
}

object WebServer extends App with RestApi {

  val (host, port) = {
    val config = system.settings.config.getConfig("robo-voice-system")
    val host = config.getString("hostname")
    val port = config.getInt("port")
    (host, port)
  }

  val bindingFuture = Http().bindAndHandle(route, host, port)
  println(s"Server online at http://$host:$port/\n")
//  println(s"Server online at http://$host:$port/\nPress RETURN to stop...")
//  scala.io.StdIn.readLine()

//  bindingFuture
//    .flatMap(_.unbind())
//    .onComplete(_ => system.terminate())
}
