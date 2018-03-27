package com.sksamuel.elastic4s.akkahttp

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.{HttpEntity => AkkaHttpEntity}
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.http.HttpEntity.StringEntity
import com.sksamuel.elastic4s.http.{HttpEntity, HttpRequestClient, HttpResponse}

import scala.concurrent.Future
import scala.util.Random

class AkkaHttpBackend(hosts: Seq[(String, Int)]) extends HttpRequestClient {

  implicit val system           = ActorSystem()
  implicit val materializer     = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  private def getRandomHostUri = ElasticsearchClientUri(Random.shuffle(hosts).head._1, Random.shuffle(hosts).head._2)

  private def request(
    method: String,
    endpoint: String,
    params: Map[String, Any],
    entity: akka.http.scaladsl.model.RequestEntity = akka.http.scaladsl.model.HttpEntity.Empty
  ): HttpRequest = {
    val met: HttpMethod = method.toUpperCase match {
      case "PUT"    => HttpMethods.PUT
      case "GET"    => HttpMethods.GET
      case "POST"   => HttpMethods.POST
      case "HEAD"   => HttpMethods.HEAD
      case "DELETE" => HttpMethods.DELETE
    }
    val endpointFormatter = new StringBuilder(endpoint)
    if (endpointFormatter.charAt(0) != '/')
      endpointFormatter.insert(0, "/")

    val host = getRandomHostUri

    val requestUri = s"${host.uri}${endpointFormatter.mkString.replace("%2B", "%25")}?${params
      .map { case (k, v) => k + "=" + v }
      .mkString("&")}"
      .stripSuffix("?")
      .stripPrefix("elasticsearch://")

    //Debug prints
    //println("Endpoint=" + endpointFormatter.mkString)
    //println("URI=" + requestUri)
    //println("Method=" + method)
    //println("Parameters=" + params)
    //println("RequestEntity=" + entity)

    val uri: Uri = Uri(requestUri, Uri.ParsingMode.Relaxed)

    HttpRequest(method = met, uri = requestUri, entity = entity)
  }

  def concat(a: ByteString, b: ByteString): ByteString = a ++ b

  private def processResponse(f: Future[akka.http.scaladsl.model.HttpResponse]): Future[HttpResponse] = {
    val response = for {
      resp <- f
      data <- resp.entity.dataBytes.runWith(Sink.fold(ByteString())(concat)).map(_.decodeString("UTF-8"))
    } yield
      HttpResponse(
        resp.status.intValue(),
        Some(StringEntity(data, resp.headers.find(_.is("content-type")).map(_.value()))),
        resp.headers.map(x => (x.name(), x.value())).toMap
      )
    response
  }

  override def async(method: String, endpoint: String, params: Map[String, Any]): Future[HttpResponse] = {
    val response = Http().singleRequest(request(method, endpoint, params))
    processResponse(response)
  }

  override def async(method: String,
                     endpoint: String,
                     params: Map[String, Any],
                     entity: HttpEntity): Future[HttpResponse] = {
    var reqEntity: RequestEntity = entity match {
      case StringEntity(content: String, contentType: Option[String]) =>
        AkkaHttpEntity(ContentTypes.`application/json`, content)
      case _ => ???
    }
    val req      = request(method, endpoint, params, reqEntity)
    val response = Http().singleRequest(req)
    processResponse(response)
  }

  override def close(): Unit = ???
}

object AkkaHttpBackend {
  def apply(hosts: Seq[(String, Int)]): AkkaHttpBackend    = new AkkaHttpBackend(hosts)
  def apply(host: ElasticsearchClientUri): AkkaHttpBackend = new AkkaHttpBackend(host.hosts)
}
