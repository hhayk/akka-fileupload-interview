import SupervisorDownloadImage.DownloadImages
import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class ProcessImageSpec()
  extends TestKit(ActorSystem("robo-voice-system"))
    with ImplicitSender
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "An Echo actor" must {
    "send back messages unchanged" in {
      val echo = system.actorOf(Props[SupervisorDownloadImage])
      echo ! DownloadImages(Set("https://farm3.staticflickr.com/2879/11234651086_681b3c2c00_b_d.jpg"))
      expectMsg("Starting")
    }
  }
}
