package com.codeheadsystems.motif.server;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SecurityHeadersFilterTest {

  private final SecurityHeadersFilter filter = new SecurityHeadersFilter();

  @Test
  void setsSecurityHeadersOnHttpResponse() throws Exception {
    ServletRequest request = mock(ServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    verify(response).setHeader("X-Content-Type-Options", "nosniff");
    verify(response).setHeader("X-Frame-Options", "DENY");
    verify(response).setHeader("Content-Security-Policy", "frame-ancestors 'none'");
    verify(response).setHeader("X-XSS-Protection", "1; mode=block");
    verify(response).setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
    verify(chain).doFilter(request, response);
  }

  @Test
  void passesNonHttpResponseThrough() throws Exception {
    ServletRequest request = mock(ServletRequest.class);
    jakarta.servlet.ServletResponse response = mock(jakarta.servlet.ServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    verifyNoInteractions(response);
    verify(chain).doFilter(request, response);
  }
}
