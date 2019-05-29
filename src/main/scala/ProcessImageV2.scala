import java.io.File

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import spray.json.DefaultJsonProtocol

import scala.concurrent.Future

object ProcessImageV2JsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
}

class ProcessImageV2 extends {
  def createTempFolderIfNotExist(): Unit = {
    val dir = new File(ImgurConfiguration.directory)
    if (!dir.exists()) dir.mkdirs()
  }

  def processImage(urls: Set[String])(implicit system: ActorSystem) = {
    implicit val materializer = ActorMaterializer()
    implicit val ec = system.dispatcher

    val log = system.log
    val cores = Runtime.getRuntime.availableProcessors()
    val downloadFutures = urls.map(uri => {
      log.info(s"Download at uri : $uri")

      ImgurClient.download(uri)(system)
    })
    val uploadFutures = (fileName: String) => {
      log.info(s"Upload file with name : $fileName")

      ImgurClient.upload(fileName)(system)
    }

    createTempFolderIfNotExist()

    val source = Source(downloadFutures)
    val flow1 = Flow[Future[String]].mapAsync[String](cores)(identity)
    val flow2 = Flow[String].map(uploadFutures)
    val flow3 = Flow[Future[ImgurResponse]].mapAsync[ImgurResponse](cores)(identity)
    val flow4 = Flow[ImgurResponse].map {
      case resp: ImgurResponseSuccess => {
        log.info(s"Success Upload Image With Link : ${resp.link}")

        resp.link
      }
      case resp: ImgurResponseFailure => {
        log.info(s"Failure Upload Image With Message : ${resp.message}")

        resp.message
      }
    }
    val sink = Sink.fold[List[String], String](Nil) { (acc, i) => acc :+ i }

    source.via(flow1).via(flow2).via(flow3).via(flow4).runWith(sink)
  }
}
