package com.codeheadsystems.motif.common;

import java.util.List;
import java.util.Optional;

/**
 * A page of results from a list query.
 *
 * @param items      the items in this page.
 * @param pageNumber the zero-based page number.
 * @param pageSize   the maximum number of items per page.
 * @param hasMore    whether more pages are available after this one.
 * @param <T>        the item type.
 */
public record Page<T>(List<T> items, int pageNumber, int pageSize, boolean hasMore) {

  /**
   * Build a Page from an over-fetched list. The list should contain up to {@code pageSize + 1}
   * items. If the extra item is present, {@code hasMore} is true and the extra item is trimmed.
   */
  public static <T> Page<T> of(List<T> overFetchedItems, PageRequest pageRequest) {
    boolean hasMore = overFetchedItems.size() > pageRequest.pageSize();
    List<T> items = hasMore
        ? overFetchedItems.subList(0, pageRequest.pageSize())
        : overFetchedItems;
    return new Page<>(List.copyOf(items), pageRequest.pageNumber(), pageRequest.pageSize(), hasMore);
  }

  public boolean isEmpty() {
    return items.isEmpty();
  }

  /**
   * Returns a {@link PageRequest} for the next page, or empty if there are no more pages.
   */
  public Optional<PageRequest> nextPageRequest() {
    if (hasMore) {
      return Optional.of(new PageRequest(pageNumber + 1, pageSize));
    }
    return Optional.empty();
  }
}
