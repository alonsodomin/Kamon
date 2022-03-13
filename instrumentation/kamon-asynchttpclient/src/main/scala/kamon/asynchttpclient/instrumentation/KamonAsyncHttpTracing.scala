package kamon.asynchttpclient.instrumentation

import kamon.Kamon
import kamon.instrumentation.http.{HttpClientInstrumentation, HttpMessage}
import org.asynchttpclient.{Request, Response}

import scala.collection.immutable.Map
import scala.collection.{JavaConverters, mutable}

object KamonAsyncHttpTracing {
  private val httpClientConfig = Kamon.config().getConfig("kamon.instrumentation.asynchttpclient.http-client")
  private val instrumentation = HttpClientInstrumentation.from(httpClientConfig, "asynchttpclient")

  def withNewSpan(request: Request): HttpClientInstrumentation.RequestHandler[Request] = {
    instrumentation.createHandler(getRequestBuilder(request), Kamon.currentContext())
  }

  def successContinuation(requestHandler: HttpClientInstrumentation.RequestHandler[Request], response: Response): Response = {
    requestHandler.processResponse(toKamonResponse(response))
    response
  }

  def failureContinuation(requestHandler: HttpClientInstrumentation.RequestHandler[Request], error: Throwable): Unit = {
    requestHandler.span.fail(error)
    requestHandler.span.finish()
  }

  def getRequestBuilder(request: Request): HttpMessage.RequestBuilder[Request] = new HttpMessage.RequestBuilder[Request]() {
    private val _headers = mutable.Map.empty[String, String]

    override def read(header: String): Option[String] =
      Option(request.getHeaders.get(header))

    override def readAll(): Map[String, String] = {
      import JavaConverters._

      request.getHeaders.entries()
        .asScala
        .map(entry => (entry.getKey, entry.getValue))
        .toMap
    }

    override def url: String = request.getUrl

    override def path: String = request.getUri.getPath

    override def method: String = request.getMethod

    override def host: String = request.getUri.getHost

    override def port: Int = request.getUri.getPort

    override def write(header: String, value: String): Unit = {
      _headers += (header -> value)
    }

    override def build(): Request = {
      val builder = request.toBuilder
      _headers.foreach { case (name, value) => builder.addHeader(name, value) }
      builder.build()
    }
  }

  def toKamonResponse(response: Response): HttpMessage.Response = new HttpMessage.Response() {
    override def statusCode: Int = response.getStatusCode
  }
}
