import java.io.File

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import spray.json.DefaultJsonProtocol

import scala.concurrent.Future

class ProcessImageV3 extends {
  def createTempFolderIfNotExist(): Unit = {
    val dir = new File(ImgurConfiguration.directory)
    if (!dir.exists()) dir.mkdirs()
  }

  def processImage(urls: Set[String])(implicit system: ActorSystem) = {
    implicit val materializer = ActorMaterializer()
    implicit val ec = system.dispatcher
    createTempFolderIfNotExist()

    Source(urls.map(ImgurClient.download))
      .via(Flow[Future[String]].map(_.flatMap(ImgurClient.upload)))
      .via(Flow[Future[ImgurResponse]].mapAsync[ImgurResponse](Runtime.getRuntime.availableProcessors())(identity))
      .runFold(List[String]())((acc, i) => i match {
        case resp: ImgurResponseSuccess => {
          system.log.info(s"Success Upload Image With Link : ${resp.link}")

          acc :+ resp.link
        }
        case resp: ImgurResponseFailure => {
          system.log.info(s"Failure Upload Image With Message : ${resp.message}")

          acc :+ resp.message
        }
      })
  }
}
