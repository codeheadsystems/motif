package com.codeheadsystems.motif.common;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Function;

public class Configuration implements Function<String, Optional<String>> {

  private final LoadingCache<String, String> cache;

  public Configuration(final Function<String, String> configurationProvider) {
    cache = Caffeine.newBuilder()
        .maximumSize(1_000)
        .expireAfterWrite(Duration.ofMinutes(10))
        .refreshAfterWrite(Duration.ofMinutes(5))
        .build(configurationProvider::apply);
  }

  @Override
  public Optional<String> apply(final String key) {
    return Optional.ofNullable(cache.get(key));
  }

  /**
   * Requires the key exists and will throw an exception if it doesn't.
   *
   * @param key in the configuration.
   * @return the value
   * @throws IllegalArgumentException if the key is not found.
   */
  public String get(final String key) {
    return apply(key).orElseThrow(() -> new IllegalArgumentException("Configuration key not found: " + key));
  }


}
