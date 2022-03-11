package kamon.asynchttpclient.instrumentation

import java.io.IOException

import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}
import kamon.Kamon
import kamon.context.Context
import kamon.asynchttpclient.utils.{JettySupport, ServletTestSupport}
import kamon.tag.Lookups.{plain, plainBoolean, plainLong}
import kamon.testkit.{InitAndStopKamonAfterAll, Reconfigure, TestSpanReporter}
import kamon.trace.Span
import kamon.trace.SpanPropagation.B3
import org.asynchttpclient._
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.SpanSugar
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterEach, OptionValues}

class AsyncHttpClientInstrumentationSpec extends AnyWordSpec
  with Matchers
  with Eventually
  with SpanSugar
  with InitAndStopKamonAfterAll
  with BeforeAndAfterEach
  with TestSpanReporter
  with JettySupport
  with Reconfigure
  with OptionValues {
  
  val customTag = "requestId"
  val customHeaderName = "X-Request-Id"

  val uriError = "/path/fail"

  "the AsyncHttpClient Tracing Instrumentation" should {
    "propagate the current context and generate a span around a request" in {
      val asyncHttpClientSpan = Kamon.spanBuilder("asynchttpclient-operation-span").start()
      val client = new DefaultAsyncHttpClient(new DefaultAsyncHttpClientConfig.Builder().build())

      val path = "path/to/resource"
      val url = s"http://$host:$port/$path"
      val request = new RequestBuilder("GET").setUrl(url).build()

      Kamon.runWithContext(Context.of(Span.Key, asyncHttpClientSpan)) {
        val response = client.executeRequest(request).get()
      }

      val span: Span.Finished = eventually(timeout(3 seconds)) {
        val span = testSpanReporter().nextSpan().value

        span.operationName shouldBe url
        span.kind shouldBe Span.Kind.Client
        span.metricTags.get(plain("component")) shouldBe "assynchttpclient"
        span.metricTags.get(plain("http.method")) shouldBe "GET"
        span.metricTags.get(plainLong("http.status_code")) shouldBe 200
        span.metricTags.get(plainBoolean("error")) shouldBe false
        span.tags.get(plain("http.url")) shouldBe url

        asyncHttpClientSpan.id shouldBe span.parentId

        testSpanReporter().nextSpan() shouldBe None

        span
      }

      val requests = consumeSentRequests()

      requests.size shouldBe 1
      requests.head.uri shouldBe path
      requests.head.header(B3.Headers.TraceIdentifier).value should be(span.trace.id.string)
      requests.head.header(B3.Headers.SpanIdentifier).value should be(span.id.string)
      requests.head.header(B3.Headers.ParentSpanIdentifier).value should be(span.parentId.string)
      requests.head.header(B3.Headers.Sampled).value should be("1")
    }

    "propagate context tags" in {
      val asyncHttpClientSpan = Kamon.internalSpanBuilder("asynchttpclient-span-with-extra-tags", "user-app").start()
      val client = new DefaultAsyncHttpClient(new DefaultAsyncHttpClientConfig.Builder().build())

      val path = "path/to/resource/extra-tags"
      val url = s"http://$host:$port/$path"
      val request = new RequestBuilder("GET").setUrl(url).build()

      Kamon.runWithContext(Context.of(Span.Key, asyncHttpClientSpan).withTag(customTag, "1234")) {
        val response = client.executeRequest(request).get()
      }

      val span: Span.Finished = eventually(timeout(3 seconds)) {
        val span = testSpanReporter().nextSpan().value

        span.operationName shouldBe url
        span.kind shouldBe Span.Kind.Client
        span.metricTags.get(plain("component")) shouldBe "okhttp-client"
        span.metricTags.get(plain("http.method")) shouldBe "GET"
        span.metricTags.get(plainLong("http.status_code")) shouldBe 200
        span.metricTags.get(plainBoolean("error")) shouldBe false
        span.tags.get(plain("http.url")) shouldBe url
        span.tags.get(plain(customTag)) shouldBe "1234"

        okSpan.id == span.parentId

        testSpanReporter().nextSpan() shouldBe None

        span
      }

      val requests = consumeSentRequests()

      requests.size should be(1)
      requests.head.uri should be(uri)
      requests.head.header(B3.Headers.TraceIdentifier).value should be(span.trace.id.string)
      requests.head.header(B3.Headers.SpanIdentifier).value should be(span.id.string)
      requests.head.header(B3.Headers.ParentSpanIdentifier).value should be(span.parentId.string)
      requests.head.header(B3.Headers.Sampled).value should be("1")
      requests.head.header(customHeaderName).value should be("1234")
    }
  }

}
