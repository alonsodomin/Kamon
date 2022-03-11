package kamon.asynchttpclient.instrumentation

import org.asynchttpclient.filter.RequestFilter
import org.asynchttpclient.filter.FilterContext
import org.asynchttpclient.AsyncHandler
import org.asynchttpclient.HttpResponseBodyPart
import io.netty.handler.codec.http.HttpHeaders
import org.asynchttpclient.HttpResponseStatus
import org.asynchttpclient.Response
import kamon.instrumentation.http.HttpClientInstrumentation

class KamonTracingRequestFilter extends RequestFilter {
  
  override def filter[T](ctx: FilterContext[T]): FilterContext[T] = {
    val request = ctx.getRequest()
    val clientRequestHandler = KamonAsyncHttpTracing.withNewSpan(request)

    new FilterContext.FilterContextBuilder[T](ctx)
      .asyncHandler(new KamonTracingAsyncHandler(ctx.getAsyncHandler().asInstanceOf[AsyncHandler[Response]]))
      .request(clientRequestHandler.request)
      .build()
  }

}

class KamonTracingAsyncHandler(kamonHandler: HttpClientInstrumentation.RequestHandler[Request], delegate: AsyncHandler[Response]) extends AsyncHandler[T] {

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