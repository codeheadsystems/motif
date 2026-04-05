package com.codeheadsystems.motif.server;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Bridges HttpOnly cookie authentication to the Authorization header
 * expected by HofmannBundle. If a {@code motif_jwt} cookie is present
 * and no Authorization header exists, this filter injects one.
 */
public class CookieAuthFilter implements Filter {

  static final String COOKIE_NAME = "motif_jwt";

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    // no-op
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    if (request instanceof HttpServletRequest httpRequest) {
      String existingAuth = httpRequest.getHeader("Authorization");
      if (existingAuth == null || existingAuth.isBlank()) {
        String tokenFromCookie = extractCookieToken(httpRequest);
        if (tokenFromCookie != null) {
          chain.doFilter(new AuthHeaderRequestWrapper(httpRequest, tokenFromCookie), response);
          return;
        }
      }
    }
    chain.doFilter(request, response);
  }

  @Override
  public void destroy() {
    // no-op
  }

  private String extractCookieToken(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) return null;
    for (Cookie cookie : cookies) {
      if (COOKIE_NAME.equals(cookie.getName())) {
        String value = cookie.getValue();
        return (value != null && !value.isBlank()) ? value : null;
      }
    }
    return null;
  }

  /**
   * Wraps the original request to inject an Authorization header.
   */
  private static class AuthHeaderRequestWrapper extends HttpServletRequestWrapper {

    private static final String AUTH_HEADER = "Authorization";
    private final String bearerValue;

    AuthHeaderRequestWrapper(HttpServletRequest request, String token) {
      super(request);
      this.bearerValue = "Bearer " + token;
    }

    @Override
    public String getHeader(String name) {
      if (AUTH_HEADER.equalsIgnoreCase(name)) {
        return bearerValue;
      }
      return super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
      if (AUTH_HEADER.equalsIgnoreCase(name)) {
        return Collections.enumeration(List.of(bearerValue));
      }
      return super.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
      List<String> names = new java.util.ArrayList<>(Collections.list(super.getHeaderNames()));
      if (!names.stream().anyMatch(n -> n.equalsIgnoreCase(AUTH_HEADER))) {
        names.add(AUTH_HEADER);
      }
      return Collections.enumeration(names);
    }
  }
}
