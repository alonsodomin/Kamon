package kamon.asynchttpclient.instrumentation

import java.io.IOException

import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}
import kamon.Kamon
import kamon.context.Context
import kamon.okhttp3.utils.{JettySupport, ServletTestSupport}
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
  
}
