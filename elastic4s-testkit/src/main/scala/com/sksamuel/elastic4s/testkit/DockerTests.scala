package com.sksamuel.elastic4s.testkit

import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.akkahttp.AkkaHttpBackend
import com.sksamuel.elastic4s.http.HttpClient

trait DockerTests extends com.sksamuel.elastic4s.http.ElasticDsl {
  //val http = HttpClient(ElasticsearchClientUri("http://localhost:9200"))
  val host = List[(String, Int)]().::("http://test1", 9200).::("http://test2", 9200)
  val http = HttpClient(new AkkaHttpBackend(host))

}
