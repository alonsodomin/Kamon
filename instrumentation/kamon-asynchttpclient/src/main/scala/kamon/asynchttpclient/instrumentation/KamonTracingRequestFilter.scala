package kamon.asynchttpclient.instrumentation

import org.asynchttpclient.filter.RequestFilter
import org.asynchttpclient.filter.FilterContext
import org.asynchttpclient.HttpResponseBodyPart
import org.asynchttpclient.HttpResponseStatus
import org.asynchttpclient.{AsyncHandler, Request, Response}
import io.netty.handler.codec.http.HttpHeaders
import kamon.instrumentation.http.HttpClientInstrumentation

class KamonTracingRequestFilter extends RequestFilter {
  
  override def filter[T](ctx: FilterContext[T]): FilterContext[T] = {
    val request = ctx.getRequest
    val clientRequestHandler = KamonAsyncHttpTracing.withNewSpan(request)

    val asyncHandler: AsyncHandler[Response] = ctx.getAsyncHandler.asInstanceOf[AsyncHandler[Response]]
    new FilterContext.FilterContextBuilder[T](ctx)
      .asyncHandler(new KamonTracingAsyncHandler(clientRequestHandler, asyncHandler))
      .request(clientRequestHandler.request)
      .build()
  }

}

class KamonTracingAsyncHandler(
    kamonHandler: HttpClientInstrumentation.RequestHandler[Request],
    delegate: AsyncHandler[Response]
  ) extends AsyncHandler[Response] {

  override def onThrowable(t: Throwable): Unit = {
    KamonAsyncHttpTracing.failureContinuation(kamonHandler, t)
    delegate.onThrowable(t)
  }

  override def onBodyPartReceived(bodyPart: HttpResponseBodyPart): AsyncHandler.State =
    delegate.onBodyPartReceived(bodyPart)

  override def onHeadersReceived(headers: HttpHeaders): AsyncHandler.State =
    delegate.onHeadersReceived(headers)

  override def onStatusReceived(responseStatus: HttpResponseStatus): AsyncHandler.State =
    delegate.onStatusReceived(responseStatus)

  override def onCompleted(): Response = {
    KamonAsyncHttpTracing.successContinuation(kamonHandler, delegate.onCompleted())
  }
}