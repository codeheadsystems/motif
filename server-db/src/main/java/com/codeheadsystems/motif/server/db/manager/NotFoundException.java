package com.codeheadsystems.motif.server.db.manager;

/**
 * Thrown when an update targets an entity that does not exist.
 */
public class NotFoundException extends RuntimeException {

  public NotFoundException(final String message) {
    super(message);
  }
}
