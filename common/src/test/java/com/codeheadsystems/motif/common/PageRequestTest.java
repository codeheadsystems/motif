package com.codeheadsystems.motif.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PageRequestTest {

  @Test
  void firstReturnsDefaultPageSize() {
    PageRequest pr = PageRequest.first();
    assertThat(pr.pageNumber()).isZero();
    assertThat(pr.pageSize()).isEqualTo(50);
  }

  @Test
  void firstWithCustomPageSize() {
    PageRequest pr = PageRequest.first(10);
    assertThat(pr.pageNumber()).isZero();
    assertThat(pr.pageSize()).isEqualTo(10);
  }

  @Test
  void nextIncrementsPageNumber() {
    PageRequest pr = PageRequest.first(10).next();
    assertThat(pr.pageNumber()).isEqualTo(1);
    assertThat(pr.pageSize()).isEqualTo(10);
  }

  @Test
  void offsetCalculatedCorrectly() {
    assertThat(PageRequest.first(10).offset()).isZero();
    assertThat(PageRequest.first(10).next().offset()).isEqualTo(10);
    assertThat(new PageRequest(3, 25).offset()).isEqualTo(75);
  }

  @Test
  void rejectsNegativePageNumber() {
    assertThatThrownBy(() -> new PageRequest(-1, 10))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsZeroPageSize() {
    assertThatThrownBy(() -> new PageRequest(0, 0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsPageSizeAboveMax() {
    assertThatThrownBy(() -> new PageRequest(0, PageRequest.MAX_PAGE_SIZE + 1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void acceptsMaxPageSize() {
    PageRequest pr = new PageRequest(0, PageRequest.MAX_PAGE_SIZE);
    assertThat(pr.pageSize()).isEqualTo(200);
  }
}
