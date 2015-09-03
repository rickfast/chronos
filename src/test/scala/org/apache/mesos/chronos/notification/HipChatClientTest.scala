package org.apache.mesos.chronos.notification

import java.net.URL

import akka.actor.ActorSystem
import akka.testkit.TestActorRef
import com.typesafe.config.ConfigFactory
import org.apache.commons.lang.RandomStringUtils
import org.apache.mesos.chronos.scheduler.jobs.BaseJob
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.specs2.mock.Mockito

import org.mockito.Mockito._
import org.junit.Assert._
import RandomStringUtils._

class HipChatClientTest extends Mockito {

  implicit val system = ActorSystem("TestActorSystem", ConfigFactory.load())

  @Test
  def shouldPostToUser(): Unit = {
    val url = s"http://${random(20)}/"
    val token = random(20)
    val client = spy(TestActorRef(new HipChatClient(url, token)).underlyingActor)
    val user = s"${random(10)}@${random(10)}.com"
    doNothing().when(client).postMessage(any[URL], any[Array[Byte]])

    client.sendNotification(mock[BaseJob],
      user, RandomStringUtils.random(10), None)

    val urlCaptor = ArgumentCaptor.forClass(classOf[URL])
    val jsonCaptor = ArgumentCaptor.forClass(classOf[Array[Byte]])

    verify(client, times(1)).postMessage(urlCaptor.capture(), jsonCaptor.capture())

    val postedUrl = urlCaptor.getValue.toExternalForm

    assertTrue(postedUrl.contains(url))
    assertTrue(postedUrl.contains(s"/user/$user/message"))
    assertTrue(postedUrl.contains(s"auth_token=$token"))
  }
}
