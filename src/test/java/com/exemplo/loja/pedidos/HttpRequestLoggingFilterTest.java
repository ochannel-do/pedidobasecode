package com.exemplo.loja.pedidos;

import static org.assertj.core.api.Assertions.*;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.exemplo.loja.pedidos.infrastructure.web.HttpRequestLoggingFilter;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class HttpRequestLoggingFilterTest {
  @Test
  void fallbackELimpezaMesmoComExcecao() throws Exception {
    var filter =
        new HttpRequestLoggingFilter(
            new DefaultListableBeanFactory().getBeanProvider(Tracer.class));
    var logger = (Logger) LoggerFactory.getLogger(HttpRequestLoggingFilter.class);
    var events =
        new ListAppender<ILoggingEvent>() {
          @Override
          protected void append(ILoggingEvent event) {
            event.prepareForDeferredProcessing();
            super.append(event);
          }
        };
    events.start();
    logger.addAppender(events);
    String previous = MDC.get("correlationId");
    try {
      MDC.remove("correlationId");
      var first = new MockHttpServletResponse();
      filter.doFilter(
          new MockHttpServletRequest("GET", "/private@example.com"),
          first,
          (request, response) ->
              assertThat(MDC.get("correlationId")).isEqualTo(first.getHeader("X-Correlation-Id")));
      assertThat(MDC.get("correlationId")).isNull();
      MDC.put("correlationId", "outer-context");
      var second = new MockHttpServletResponse();
      assertThatThrownBy(
              () ->
                  filter.doFilter(
                      new MockHttpServletRequest("POST", "/pedidos"),
                      second,
                      (request, response) -> {
                        throw new ServletException("falha");
                      }))
          .isInstanceOf(ServletException.class);
      assertThat(MDC.get("correlationId")).isEqualTo("outer-context");
      assertThat(first.getHeader("X-Correlation-Id"))
          .isNotEqualTo(second.getHeader("X-Correlation-Id"));
      assertThat(events.list).hasSize(2);
      assertThat(events.list.getFirst().getMDCPropertyMap().get("correlationId"))
          .isEqualTo(first.getHeader("X-Correlation-Id"));
      assertThat(events.list.getLast().getKeyValuePairs())
          .anySatisfy(
              kv -> {
                assertThat(kv.key).isEqualTo("status");
                assertThat(kv.value).isEqualTo(500);
              });
      assertThat(events.list.getFirst().getKeyValuePairs())
          .anySatisfy(
              kv -> {
                assertThat(kv.key).isEqualTo("path");
                assertThat(kv.value).isEqualTo("UNMATCHED");
              });
    } finally {
      logger.detachAppender(events);
      events.stop();
      if (previous == null) MDC.remove("correlationId");
      else MDC.put("correlationId", previous);
    }
  }
}
