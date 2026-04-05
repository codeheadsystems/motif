package com.codeheadsystems.motif.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CookieAuthFilterTest {

  private final CookieAuthFilter filter = new CookieAuthFilter();

  @Test
  void injectsAuthHeaderFromCookie() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    when(request.getHeader("Authorization")).thenReturn(null);
    when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("motif_jwt", "my-token")});

    filter.doFilter(request, response, chain);

    ArgumentCaptor<ServletRequest> captor = ArgumentCaptor.forClass(ServletRequest.class);
    verify(chain).doFilter(captor.capture(), eq(response));
    HttpServletRequest wrappedRequest = (HttpServletRequest) captor.getValue();
    assertThat(wrappedRequest.getHeader("Authorization")).isEqualTo("Bearer my-token");
  }

  @Test
  void doesNotOverrideExistingAuthHeader() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    when(request.getHeader("Authorization")).thenReturn("Bearer existing");

    filter.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
  }

  @Test
  void passesThrough_whenNoCookie() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    when(request.getHeader("Authorization")).thenReturn(null);
    when(request.getCookies()).thenReturn(null);

    filter.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
  }

  @Test
  void passesThrough_whenCookieValueIsBlank() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    when(request.getHeader("Authorization")).thenReturn(null);
    when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("motif_jwt", "  ")});

    filter.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
  }

  @Test
  void passesThrough_whenNonHttpRequest() throws Exception {
    ServletRequest request = mock(ServletRequest.class);
    ServletResponse response = mock(ServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
  }
}
