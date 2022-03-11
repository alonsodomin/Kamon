package kamon.asynchttpclient.instrumentation

import kamon.instrumentation.http.{HttpMessage, HttpOperationNameGenerator}

class AsyncHttpClientOperationNameGenerator extends HttpOperationNameGenerator {
  override def name(request: HttpMessage.Request): Option[String] = {
    Option(request.url)
  }
}
