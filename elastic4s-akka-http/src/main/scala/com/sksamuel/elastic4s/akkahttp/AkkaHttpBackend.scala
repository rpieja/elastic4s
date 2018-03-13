package com.sksamuel.elastic4s.akkahttp

import akka.actor.ActorSystem
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
  // needed for the future flatMap/onComplete in the end
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
    val sb = new StringBuilder(endpoint)
    if (sb.charAt(0) != '/')
      sb.insert(0, "/")

    //Work in progress URI Builder TODO: Sensible uri concat tool

    val requestUri = s"${elasticsearchClientUri.uri}$endpoint?${params.map { case (k, v) => k + "=" + v }.mkString("&")}"

    //println(requestUri)
    //OLD URI BUILDER
//    val requestHost: String = elasticsearchClientUri.uri + sb.mkString
//    val parameters: String =
//      params.map { case (k, v) => s"${k}=${v}" }.mkString("&")
//    val uriString: String = requestHost + "?" + parameters
//
//    val requestUri = Uri(uriString)
//    println("URI: " + requestUri.toString())


    //    println("Uri: " + elasticsearchClientUri.uri)
    //    println("Endpoint: " + endpoint)
    //    println("Parameters: " + parameters)
    //    println("Final uri: " + fin)
    //    println("Entity: " + entity + "\n-----")
    //    println(requestUri)
    HttpRequest(method = met, uri = requestUri, entity = entity)
  }

  def concat(a: ByteString, b: ByteString): ByteString = a ++ b

  private def processResponse(f: Future[akka.http.scaladsl.model.HttpResponse]): Future[HttpResponse] = {
    //    f.map { resp =>
    //      println("Status code: " + resp.status.intValue())
    //    }
    val response = for {
      resp <- f
      data <- resp.entity.dataBytes.runWith(Sink.fold(ByteString())(concat)).map(_.decodeString("UTF-8"))
    } yield HttpResponse(
      resp.status.intValue(),
      Some(StringEntity(data, resp.headers.find(_.is("content-type")).map(_.value()))),
      resp.headers.map(x => (x.name(), x.value())).toMap
    )
    response.andThen { case x => println("RESPONSE:\n" + x.get.entity + "\n-------") }
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
