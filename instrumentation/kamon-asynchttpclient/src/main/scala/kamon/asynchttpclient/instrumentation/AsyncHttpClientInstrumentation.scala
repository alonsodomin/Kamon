package kamon.asynchttpclient.instrumentation

import org.asynchttpclient.AsyncHttpClientConfig
import kanela.agent.api.instrumentation.InstrumentationBuilder
import kanela.agent.libs.net.bytebuddy.asm.Advice
import org.asynchttpclient.DefaultAsyncHttpClientConfig
import scala.collection.JavaConverters

class AsyncHttpClientInstrumentation extends InstrumentationBuilder {
  
  /**
    * Instrument:
    *
    * org.asynchttpclient.DefaultAsyncHttpClient::constructor
    */
  onType("org.asynchttpclient.DefaultAsyncHttpClient")
    .advise(isConstructor() and takesOneArgumentOf("org.asynchttpclient.AsyncHttpClientConfig"), classOf[AsyncHttpClientAdvisor])

}

class AsyncHttpClientAdvisor
object AsyncHttpClientAdvisor {
  import JavaConverters._

  @Advice.OnMethodEnter(suppress = classOf[Throwable])
  @Advice.Returned.ToArguments(Advice.AssignReturned.ToArgument(0))
  def addKamonInterceptor(@Advice.Argument(0) config: AsyncHttpClientConfig): AsyncHttpClientConfig = {
    val requestFilters = config.getRequestFilters().asScala

    if (!requestFilters.exists(_.isInstanceOf[KamonTracingRequestFilter])) {
      val builder = new DefaultAsyncHttpClientConfig.Builder(config)
        .addRequestFilter(new KamonTracingRequestFilter())

      builder.build()
    } else config
  }
}