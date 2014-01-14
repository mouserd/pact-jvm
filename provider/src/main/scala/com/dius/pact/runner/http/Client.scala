package com.dius.pact.runner.http

import com.dius.pact.model.{Response, Request}
import scala.concurrent.Future
import spray.http._
import spray.client.pipelining._
import akka.actor.ActorSystem
import spray.http.HttpHeaders.RawHeader

class Client {

  implicit val system = ActorSystem()
  import system.dispatcher // execution context for futures
  val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

  def convertRequest(baseUrl:String, request:Request):HttpRequest = {
    val method = HttpMethods.getForKey(request.method.toString.toUpperCase).get
    val uri = Uri(s"$baseUrl${request.path}")
    val headers: List[HttpHeader] = request.headers.map(_.toList.map{case (key, value) => RawHeader(key, value)}).getOrElse(Nil)
    val entity: HttpEntity = request.bodyString.map(HttpEntity(_)).getOrElse(HttpEntity.Empty)
    HttpRequest(method, uri, headers, entity)
  }

  def invoke(baseUrl:String, request:Request):Future[Response] =  {
    pipeline(convertRequest(baseUrl, request)).map(com.dius.pact.model.spray.Conversions.sprayToPactResponse)
  }

  def invoke(url:String, body:String):Future[Boolean] = {
    pipeline(Post(url, body)).map(r => r.status.isSuccess)
  }
}