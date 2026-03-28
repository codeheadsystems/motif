package com.codeheadsystems.motif.server.db;

import com.codeheadsystems.motif.model.Category;
import com.codeheadsystems.motif.model.Identifier;
import com.codeheadsystems.motif.model.Subject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

@RegisterRowMapper(SubjectDao.SubjectRowMapper.class)
public interface SubjectDao {

  // --- SQL-level methods ---

  @SqlUpdate("INSERT INTO subjects (identifier_uuid, category, value) "
      + "VALUES (:uuid, :category, :value) "
      + "ON CONFLICT (identifier_uuid) DO UPDATE SET "
      + "category = EXCLUDED.category, "
      + "value = EXCLUDED.value")
  void upsert(@Bind("uuid") UUID uuid,
              @Bind("category") String category,
              @Bind("value") String value);

  @SqlQuery("SELECT * FROM subjects WHERE identifier_uuid = :uuid")
  Optional<Subject> findByIdentifier(@Bind("uuid") UUID uuid);

  @SqlUpdate("DELETE FROM subjects WHERE identifier_uuid = :uuid")
  int deleteByIdentifier(@Bind("uuid") UUID uuid);

  @SqlQuery("SELECT * FROM subjects WHERE category = :category ORDER BY value")
  List<Subject> findByCategory(@Bind("category") String category);

  @SqlQuery("SELECT * FROM subjects WHERE category = :category AND value = :value")
  Optional<Subject> findByCategoryAndValue(@Bind("category") String category,
                                           @Bind("value") String value);

  // --- Domain-level methods ---

  default void store(Subject subject) {
    upsert(subject.identifier().uuid(),
        subject.category().value(),
        subject.value());
  }

  default Optional<Subject> get(Identifier identifier) {
    return findByIdentifier(identifier.uuid());
  }

  default boolean delete(Identifier identifier) {
    return deleteByIdentifier(identifier.uuid()) > 0;
  }

  default List<Subject> findByCategory(Category category) {
    return findByCategory(category.value());
  }

  default Optional<Subject> find(Category category, String value) {
    return findByCategoryAndValue(category.value(), value);
  }

  // --- Row mapper ---

  class SubjectRowMapper implements RowMapper<Subject> {
    @Override
    public Subject map(ResultSet rs, StatementContext ctx) throws SQLException {
      return Subject.builder(
              new Category(rs.getString("category")),
              rs.getString("value"))
          .identifier(new Identifier(rs.getObject("identifier_uuid", UUID.class)))
          .build();
    }
  }

}
