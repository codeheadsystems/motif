package com.codeheadsystems.motif.server.resource;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HelloWorldResourceTest {

  private final HelloWorldResource resource = new HelloWorldResource();

  @Test
  void helloReturnsJsonMessage() {
    String result = resource.hello();

    assertThat(result).contains("Hello, World!");
  }
}
