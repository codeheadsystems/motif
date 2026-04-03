package com.codeheadsystems.motif.common;

/**
 * Represents a request for a page of results.
 *
 * @param pageNumber zero-based page number.
 * @param pageSize   maximum number of items per page.
 */
public record PageRequest(int pageNumber, int pageSize) {

  public static final int DEFAULT_PAGE_SIZE = 50;

  public PageRequest {
    if (pageNumber < 0) {
      throw new IllegalArgumentException("pageNumber must be >= 0");
    }
    if (pageSize < 1) {
      throw new IllegalArgumentException("pageSize must be >= 1");
    }
  }

  public static PageRequest first() {
    return new PageRequest(0, DEFAULT_PAGE_SIZE);
  }

  public static PageRequest first(int pageSize) {
    return new PageRequest(0, pageSize);
  }

  public PageRequest next() {
    return new PageRequest(pageNumber + 1, pageSize);
  }

  public int offset() {
    return pageNumber * pageSize;
  }
}
