package com.codeheadsystems.motif.server.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

/**
 * Represents a single tag associated with an entity UUID. Used for batch tag loading.
 */
public record TagEntry(UUID uuid, String tagValue) {

  public static class TagEntryRowMapper implements RowMapper<TagEntry> {
    @Override
    public TagEntry map(ResultSet rs, StatementContext ctx) throws SQLException {
      return new TagEntry(
          UUID.fromString(rs.getString("uuid")),
          rs.getString("tag_value"));
    }
  }
}
