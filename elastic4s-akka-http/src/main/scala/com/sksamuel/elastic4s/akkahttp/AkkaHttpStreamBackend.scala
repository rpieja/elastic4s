package com.sksamuel.elastic4s.akkahttp

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpMethod, HttpMethods, HttpRequest, RequestEntity, Uri, HttpEntity => AkkaHttpEntity, HttpResponse => AkkaHttpResponse}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source, SourceQueueWithComplete}
import akka.util.ByteString
import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.http.HttpEntity.StringEntity
import com.sksamuel.elastic4s.http.{HttpClient, HttpEntity, HttpRequestClient, HttpResponse}

import scala.concurrent.{Future, Promise}
import scala.util.Random

class AkkaHttpStreamBackend(hosts: Seq[(String, Int)]) extends HttpRequestClient {

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
      case "PUT" => HttpMethods.PUT
      case "GET" => HttpMethods.GET
      case "POST" => HttpMethods.POST
      case "HEAD" => HttpMethods.HEAD
      case "DELETE" => HttpMethods.DELETE
    }
    val endpointFormatter = new StringBuilder(endpoint)
    if (endpointFormatter.charAt(0) != '/')
      endpointFormatter.insert(0, "/")

    val host = getRandomHostUri

    val requestUri = s"${host.uri}${endpointFormatter.mkString.replace("%2B", "%25")}?${
      params
        .map { case (k, v) => k + "=" + v }
        .mkString("&")
    }"
      .stripSuffix("?")
      .stripPrefix("elasticsearch://")

    val uri: Uri = Uri(requestUri, Uri.ParsingMode.Relaxed)

    HttpRequest(method = met, uri = requestUri, entity = entity)
  }

  def processRequest(httpRequest: HttpRequest): Future[AkkaHttpResponse] = {
    val httpRequestStream: SourceQueueWithComplete[(HttpRequest, Promise[AkkaHttpResponse])] = Source
      .queue[(HttpRequest, Promise[AkkaHttpResponse])](2048, OverflowStrategy.backpressure)
      .via(Http().superPool())
      .map {
        case (v, p) => p.complete(v)
      }
      .to(Sink.ignore)
      .run()
    val promise = Promise[AkkaHttpResponse]
    httpRequestStream.offer((httpRequest, promise))
    promise.future
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

  override def async(method: String,
                     endpoint: String,
                     params: Map[String, Any]): Future[HttpResponse] = {
    processResponse(processRequest(request(method, endpoint, params)))
  }

  override def async(method: String,
                     endpoint: String,
                     params: Map[String, Any],
                     entity: HttpEntity): Future[HttpResponse] = {
    var requestEntity: RequestEntity = entity match {
      case StringEntity(content: String, contentType: Option[String]) =>
        AkkaHttpEntity(ContentTypes.`application/json`, content)
      case _ => ???
    }
    processResponse(processRequest(request(method, endpoint, params, requestEntity)))
  }

  override def close(): Unit = ???
}
object AkkaHttpStreamBackend {
  def apply(hosts: Seq[(String, Int)]): AkkaHttpBackend = new AkkaHttpBackend(hosts)

  def apply(host: ElasticsearchClientUri): AkkaHttpBackend = new AkkaHttpBackend(host.hosts)
}
