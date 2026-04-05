package com.codeheadsystems.motif.server;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class SecurityHeadersFilter implements Filter {

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    // no-op
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    if (response instanceof HttpServletResponse httpResponse) {
      httpResponse.setHeader("X-Content-Type-Options", "nosniff");
      httpResponse.setHeader("X-Frame-Options", "DENY");
      httpResponse.setHeader("Content-Security-Policy", "frame-ancestors 'none'");
      httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
      httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
    }
    chain.doFilter(request, response);
  }

  @Override
  public void destroy() {
    // no-op
  }
}
