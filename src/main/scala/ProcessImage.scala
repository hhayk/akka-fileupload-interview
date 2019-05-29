import java.io.File

import SlaveDownloadImage.DownloadImageUrl
import SlaveUploadImage.UploadImage
import SupervisorDownloadImage.{DownloadImageComplete, DownloadImages, UploadImageComplete, UploadImageFailure}
import akka.actor.{Actor, ActorLogging, Props}
import akka.stream.ActorMaterializer

import scala.concurrent.Future

abstract class AbstractDownloadImage extends Actor with ActorLogging {
  lazy val directory = context.system.settings.config.getConfig("robo-voice-system").getString("tempDir")
  implicit val materializer = ActorMaterializer()
  implicit val ec = context.system.dispatcher
}

object SupervisorDownloadImage {

  case class DownloadImages(urls: Set[String])

  case class DownloadImageComplete(fileName: String)

  case class UploadImageComplete(fileName: String)

  case class UploadImageFailure(fileName: String)

}

class SupervisorDownloadImage extends AbstractDownloadImage {
  def receive: Receive = {
    case DownloadImages(urls: Set[String]) => {
      createTempFolderIfNotExist()

      urls.foreach(url => {
        context.actorOf(Props[SlaveDownloadImage]) ! DownloadImageUrl(url)
      })
    }

    case DownloadImageComplete(fileName: String) => {
      context.actorOf(Props[SlaveUploadImage]) ! UploadImage(fileName)
    }

    case UploadImageComplete(fileName: String) => {
      cleanup(fileName, directory)
    }

    case UploadImageFailure(fileName: String) => {
      cleanup(fileName, directory)
    }
  }

  def createTempFolderIfNotExist(): Unit = {
    val dir = new File(directory)
    if (!dir.exists()) dir.mkdirs()
  }

  def cleanup(fileName: String, downloadDir: String): Unit = {
    val file = new File(downloadDir, fileName)
    if (file.exists()) {
      log.info(s"Cleanup Local Copy Image With Name : $fileName")

      file.delete()
    }
  }
}

object SlaveDownloadImage {

  case class DownloadImageUrl(uri: String)

}

class SlaveDownloadImage extends AbstractDownloadImage {
  def receive: Receive = {
    case DownloadImageUrl(uri) => {
      log.info(s"Start Download Image at URL : $uri")

      val originalSender = sender
      ImgurClient.download(uri, directory)(context.system).flatMap {
        fileName: String => {
          log.info(s"Complete Download Image With Name : $fileName")

          originalSender ! DownloadImageComplete(fileName)
          Future(fileName)
        }
      }
    }
  }
}

object SlaveUploadImage {

  case class UploadImage(fileName: String)

}

class SlaveUploadImage extends AbstractDownloadImage with JsonSupport {
  def receive: Receive = {
    case UploadImage(fileName: String) => {
      log.info(s"Start Upload Image : $fileName")

      val originalSender = sender
      ImgurClient.upload(fileName)(context.system).flatMap {
        case ImgurResponseSuccess(_, link, _) => {
          log.info(s"Success Upload Image With Link : $link")

          originalSender ! UploadImageComplete(fileName)
          Future(link)
        }
        case ImgurResponseFailure(_, message, _) => {
          log.info(s"Failure Upload Image With Message : $message")

          originalSender ! UploadImageFailure(fileName)
          Future()
        }
      }
    }
  }
}