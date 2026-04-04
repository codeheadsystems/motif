package com.codeheadsystems.motif.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConfigurationTest {

  private Configuration configuration;

  @BeforeEach
  void setUp() {
    Map<String, String> values = Map.of(
        "db.host", "localhost",
        "db.port", "5432"
    );
    configuration = new Configuration(values::get);
  }

  @Test
  void applyReturnsValueWhenKeyExists() {
    Optional<String> result = configuration.apply("db.host");

    assertThat(result).contains("localhost");
  }

  @Test
  void applyReturnsEmptyWhenKeyDoesNotExist() {
    Optional<String> result = configuration.apply("missing.key");

    assertThat(result).isEmpty();
  }

  @Test
  void getReturnsValueWhenKeyExists() {
    String result = configuration.get("db.port");

    assertThat(result).isEqualTo("5432");
  }

  @Test
  void getThrowsWhenKeyDoesNotExist() {
    assertThatThrownBy(() -> configuration.get("missing.key"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("missing.key");
  }

  @Test
  void cachedValuesAreConsistent() {
    String first = configuration.get("db.host");
    String second = configuration.get("db.host");

    assertThat(first).isEqualTo(second);
  }
}
