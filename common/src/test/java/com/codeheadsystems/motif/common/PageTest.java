package com.codeheadsystems.motif.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PageTest {

  @Test
  void ofTrimsExtraItemAndSetsHasMore() {
    PageRequest pr = PageRequest.first(2);
    Page<String> page = Page.of(List.of("a", "b", "c"), pr);

    assertThat(page.items()).containsExactly("a", "b");
    assertThat(page.hasMore()).isTrue();
    assertThat(page.pageNumber()).isZero();
    assertThat(page.pageSize()).isEqualTo(2);
  }

  @Test
  void ofWithExactPageSizeHasNoMore() {
    PageRequest pr = PageRequest.first(3);
    Page<String> page = Page.of(List.of("a", "b", "c"), pr);

    assertThat(page.items()).containsExactly("a", "b", "c");
    assertThat(page.hasMore()).isFalse();
  }

  @Test
  void ofWithFewerItemsHasNoMore() {
    PageRequest pr = PageRequest.first(5);
    Page<String> page = Page.of(List.of("a", "b"), pr);

    assertThat(page.items()).containsExactly("a", "b");
    assertThat(page.hasMore()).isFalse();
  }

  @Test
  void emptyPage() {
    PageRequest pr = PageRequest.first(10);
    Page<String> page = Page.of(List.of(), pr);

    assertThat(page.isEmpty()).isTrue();
    assertThat(page.hasMore()).isFalse();
  }

  @Test
  void nextPageRequestPresentWhenHasMore() {
    PageRequest pr = PageRequest.first(2);
    Page<String> page = Page.of(List.of("a", "b", "c"), pr);

    Optional<PageRequest> next = page.nextPageRequest();
    assertThat(next).isPresent();
    assertThat(next.get().pageNumber()).isEqualTo(1);
    assertThat(next.get().pageSize()).isEqualTo(2);
  }

  @Test
  void nextPageRequestEmptyWhenNoMore() {
    PageRequest pr = PageRequest.first(5);
    Page<String> page = Page.of(List.of("a", "b"), pr);

    assertThat(page.nextPageRequest()).isEmpty();
  }
}
