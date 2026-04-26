package com.codeheadsystems.motif.server.db.model;

/**
 * Project lifecycle states. The set is intentionally small and ordered by typical lifecycle
 * progression. ARCHIVED is the soft-delete state — archived projects are hidden by default
 * but can be queried explicitly.
 */
public enum ProjectStatus {
  ACTIVE,
  PAUSED,
  COMPLETED,
  ARCHIVED
}
