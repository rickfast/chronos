package org.apache.mesos.chronos.notification

import java.io.DataOutputStream
import java.net.{HttpURLConnection, URL}
import java.util.logging.Logger

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.apache.mesos.chronos.scheduler.jobs.BaseJob

/**
 * Notification client that posts messages to a HipChat room.
 * @param hipChatUrl The HipChat URL, e.g. "http://company.hipchat.com/
 * @param hipChatToken The HipChat API token
 */
class HipChatClient(hipChatUrl: String, hipChatToken: String) extends NotificationClient {

  private[this] val log = Logger.getLogger(getClass.getName)

  /**
   * Jackson JSON serializer with scala module
   * registered.
   */
  object Jackson {
    val mapper = new ObjectMapper()

    mapper.registerModule(DefaultScalaModule)

    def toJson(message: Map[String, Any]): Array[Byte] =
      mapper.writeValueAsBytes(message)
  }

  /**
   * Send the notification to a HipChat Room
   * @param job the job that is being notified on
   * @param to the recipient of the notification
   * @param subject the subject line to use in notification
   * @param message the message that offers additional information about the notification
   */
  override def sendNotification(job: BaseJob, to: String, subject: String, message: Option[String]): Unit = {
    val hipChatMessage = Map(
      "message" -> s"$subject: ${message.getOrElse("No message")}",
      "notify" -> true
    )
    val payload = Jackson.toJson(hipChatMessage)
    val url = new URL(s"${hipChatUrl.replaceAll("/\\z", "")}/v2/user/$to/message?auth_token=$hipChatToken")

    postMessage(url, payload)
  }

  /**
   * Posts a message to HipChat
   * @param url The HipChat URL.
   * @param payload The JSON message as bytes.
   */
  def postMessage(url: URL, payload: Array[Byte]): Unit = {
    var connection: HttpURLConnection = null

    try {
      connection = url.openConnection.asInstanceOf[HttpURLConnection]

      connection.setDoInput(true)
      connection.setDoOutput(true)
      connection.setUseCaches(false)
      connection.setRequestMethod("POST")

      val outputStream = new DataOutputStream(connection.getOutputStream)

      outputStream.write(payload)
      outputStream.flush()
      outputStream.close()
    } finally {
      if (connection != null) {
        connection.disconnect()
      }
    }
  }
}
