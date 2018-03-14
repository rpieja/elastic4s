package com.sksamuel.elastic4s.akkahttp

import akka.actor.ActorSystem
import akka.http.javadsl.model.headers.RawRequestURI
import akka.stream.ActorMaterializer
import akka.http.scaladsl.{Http, model}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.{HttpEntity => AkkaHttpEntity}
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.http.HttpEntity.{FileEntity, InputStreamEntity, StringEntity}
import com.sksamuel.elastic4s.http.{HttpEntity, HttpRequestClient, HttpResponse}

import scala.concurrent.Future
import scala.concurrent.Future

class AkkaHttpBackend(elasticsearchClientUri: ElasticsearchClientUri) extends HttpRequestClient {


  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  private def request(method: String, endpoint: String, params: Map[String, Any],
                      entity: akka.http.scaladsl.model.RequestEntity = akka.http.scaladsl.model.HttpEntity.Empty):
  HttpRequest = {
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

    val requestUri = s"${elasticsearchClientUri.uri}${endpointFormatter.mkString.replace("%2B", "%25")}?${
      params.map { case (k, v) => k + "=" + v }
        .mkString("&")
    }"
      .stripSuffix("?")

    val uri = Uri(requestUri, Uri.ParsingMode.Relaxed)


    //Debug prints
    //    println("ParsedUri="+uri)
    //println("Endpoint=" + endpointFormatter.mkString)
    //    println("URI=" + requestUri)
    //    println("Method=" + method)
    //    println("Parameters=" + parameters)
    //    println("RequestEntity=" + entity)

    HttpRequest(method = met, uri = requestUri, entity = entity)
  }

  def concat(a: ByteString, b: ByteString): ByteString = a ++ b

  private def processResponse(f: Future[akka.http.scaladsl.model.HttpResponse]): Future[HttpResponse] = {
    val response = for {
      resp <- f
      data <- resp.entity.dataBytes.runWith(Sink.fold(ByteString())(concat)).map(_.decodeString("UTF-8"))
    } yield HttpResponse(
      resp.status.intValue(),
      Some(StringEntity(data, resp.headers.find(_.is("content-type")).map(_.value()))),
      resp.headers.map(x => (x.name(), x.value())).toMap
    )
    //response.andThen { case x => println("Response: " + x.get.entity) }
    response
  }

  override def async(method: String, endpoint: String, params: Map[String, Any]): Future[HttpResponse] = {
    val response = Http().singleRequest(request(method, endpoint, params))
    processResponse(response)
  }

  override def async(method: String, endpoint: String, params: Map[String, Any], entity: HttpEntity): Future[HttpResponse] = {
    var reqEntity: RequestEntity = entity match {
      case StringEntity(content: String, contentType: Option[String]) =>
        AkkaHttpEntity(ContentTypes.`application/json`, content)
      case _ => ???
    }
    val req = request(method, endpoint, params, reqEntity)
    val response = Http().singleRequest(req)
    processResponse(response)
  }

  override def close(): Unit = ???
}
