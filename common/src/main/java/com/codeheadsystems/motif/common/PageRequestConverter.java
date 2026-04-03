package com.codeheadsystems.motif.common;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Converts {@link PageRequest} to and from a base64-encoded string representation.
 */
public final class PageRequestConverter {

  private static final String SEPARATOR = ":";

  private PageRequestConverter() {
  }

  /**
   * Encodes a {@link PageRequest} as a base64 string.
   */
  public static String encode(PageRequest pageRequest) {
    String raw = pageRequest.pageNumber() + SEPARATOR + pageRequest.pageSize();
    return Base64.getUrlEncoder().withoutPadding()
        .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Decodes a base64 string back into a {@link PageRequest}.
   *
   * @throws IllegalArgumentException if the token is invalid.
   */
  public static PageRequest decode(String token) {
    try {
      String raw = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
      String[] parts = raw.split(SEPARATOR, 2);
      if (parts.length != 2) {
        throw new IllegalArgumentException("Invalid page token");
      }
      int pageNumber = Integer.parseInt(parts[0]);
      int pageSize = Integer.parseInt(parts[1]);
      return new PageRequest(pageNumber, pageSize);
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid page token", e);
    }
  }
}
