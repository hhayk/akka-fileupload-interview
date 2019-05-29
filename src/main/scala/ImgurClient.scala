import java.io.File
import java.nio.file.Paths
import java.util.UUID.randomUUID

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, _}
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.FileIO
import com.typesafe.config.ConfigFactory
import spray.json.DefaultJsonProtocol

import scala.collection.immutable
import scala.concurrent.Future

object ImgurConfiguration {
  private val imgurConfig = ConfigFactory.load.getConfig("imgur")
  private val systemConfig = ConfigFactory.load.getConfig("robo-voice-system")

  val apiURL = imgurConfig.getString("apiURL")
  val clientId = imgurConfig.getString("clientId")
  val clientSecret = imgurConfig.getString("clientSecret")
  val token = "85b21fd71e4533a2cf68aa455db97006e036dafe"
  val directory = systemConfig.getString("tempDir")
}

trait ImgurResponse

case class ImgurResponseSuccess(id: String, link: String, title: Option[String]) extends ImgurResponse

case class ImgurResponseSuccessData(data: ImgurResponseSuccess)

case class ImgurResponseFailure(code: Int, message: String, `type`: String) extends ImgurResponse

case class ImgurResponseFailureError(error: ImgurResponseFailure)

case class ImgurResponseFailureData(data: ImgurResponseFailureError)

trait ImgurJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val imgurResponseSuccessFormat = jsonFormat3(ImgurResponseSuccess)
  implicit val imgurResponseSuccessDataFormat = jsonFormat1(ImgurResponseSuccessData)
  implicit val imgurResponseFailureFormat = jsonFormat3(ImgurResponseFailure)
  implicit val imgurResponseFailureErrorFormat = jsonFormat1(ImgurResponseFailureError)
  implicit val imgurResponseFailureDataFormat = jsonFormat1(ImgurResponseFailureData)
}

object ImgurClient extends Directives with ImgurJsonSupport {
  def destinationFile(downloadDir: String, response: HttpResponse): (File, String) = {
    val fileName = s"${randomUUID.toString}.jpg"
    val file = new File(downloadDir, fileName)
    file.createNewFile()
    (file, fileName)
  }

  def download(uri: String)(implicit system: ActorSystem): Future[String] = {
    implicit val ec = system.dispatcher
    implicit val materializer = ActorMaterializer()

    val request = HttpRequest(uri = uri)

    Http().singleRequest(request).flatMap { response =>
      val (file, fileName) = destinationFile(ImgurConfiguration.directory, response)
      val source = response.entity.dataBytes
      source.runWith(FileIO.toPath(file.toPath)) flatMap { _ =>
        Future(fileName)
      }
    }
  }

  def upload(fileName: String)(implicit system: ActorSystem): Future[ImgurResponse] = {
    implicit val ec = system.dispatcher
    implicit val materializer = ActorMaterializer()

    val file = s"${ImgurConfiguration.directory}/$fileName"

    val method = HttpMethods.POST
    val uri = s"${ImgurConfiguration.apiURL}/upload"
    val headers = immutable.Seq(Authorization(OAuth2BearerToken(ImgurConfiguration.token)))
    val entity = HttpEntity(MediaTypes.`application/octet-stream`, FileIO.fromPath(Paths.get(file)))
    val request = HttpRequest(method, uri, headers, entity)

    Http().singleRequest(request).flatMap {
      case HttpResponse(statusCode, header, entity, protocol) => {
        statusCode match {
          case StatusCodes.OK => Unmarshal(entity).to[ImgurResponseSuccessData].map(_.data)
          case _ => Unmarshal(entity).to[ImgurResponseFailureData].map(_.data.error)
        }
      }
    }
  }
}
