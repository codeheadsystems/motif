package com.codeheadsystems.motif.server.db;

import com.codeheadsystems.motif.model.Category;
import com.codeheadsystems.motif.model.Identifier;
import com.codeheadsystems.motif.model.Owner;
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

  String SELECT = "SELECT s.uuid, s.owner_uuid, s.category, s.value "
      + "FROM subjects s";

  @SqlUpdate("INSERT INTO subjects (uuid, owner_uuid, category, value) "
      + "VALUES (:uuid, :ownerUuid, :category, :value) "
      + "ON CONFLICT (uuid) DO UPDATE SET "
      + "category = EXCLUDED.category, "
      + "value = EXCLUDED.value")
  void upsert(@Bind("uuid") UUID uuid,
              @Bind("ownerUuid") UUID ownerUuid,
              @Bind("category") String category,
              @Bind("value") String value);

  @SqlQuery(SELECT + " WHERE s.owner_uuid = :ownerUuid AND s.uuid = :uuid")
  Optional<Subject> findByOwnerAndIdentifier(@Bind("ownerUuid") UUID ownerUuid,
                                              @Bind("uuid") UUID uuid);

  @SqlUpdate("DELETE FROM subjects WHERE owner_uuid = :ownerUuid AND uuid = :uuid")
  int deleteByOwnerAndIdentifier(@Bind("ownerUuid") UUID ownerUuid, @Bind("uuid") UUID uuid);

  @SqlQuery(SELECT + " WHERE s.owner_uuid = :ownerUuid AND s.category = :category "
      + "ORDER BY s.value")
  List<Subject> findByOwnerAndCategory(@Bind("ownerUuid") UUID ownerUuid,
                                        @Bind("category") String category);

  @SqlQuery(SELECT + " WHERE s.owner_uuid = :ownerUuid "
      + "AND s.category = :category AND s.value = :value")
  Optional<Subject> findByOwnerCategoryAndValue(@Bind("ownerUuid") UUID ownerUuid,
                                                 @Bind("category") String category,
                                                 @Bind("value") String value);

  default void store(Subject subject) {
    upsert(subject.identifier().uuid(),
        subject.ownerIdentifier().uuid(),
        subject.category().value(),
        subject.value());
  }

  default Optional<Subject> get(Owner owner, Identifier identifier) {
    return findByOwnerAndIdentifier(owner.identifier().uuid(), identifier.uuid());
  }

  default boolean delete(Owner owner, Identifier identifier) {
    return deleteByOwnerAndIdentifier(owner.identifier().uuid(), identifier.uuid()) > 0;
  }

  default List<Subject> findByCategory(Owner owner, Category category) {
    return findByOwnerAndCategory(owner.identifier().uuid(), category.value());
  }

  default Optional<Subject> find(Owner owner, Category category, String value) {
    return findByOwnerCategoryAndValue(owner.identifier().uuid(), category.value(), value);
  }

  class SubjectRowMapper implements RowMapper<Subject> {
    @Override
    public Subject map(ResultSet rs, StatementContext ctx) throws SQLException {
      Identifier ownerIdentifier = new Identifier(rs.getObject("owner_uuid", UUID.class));
      return Subject.builder()
          .ownerIdentifier(ownerIdentifier)
          .category(new Category(rs.getString("category")))
          .value(rs.getString("value"))
          .identifier(new Identifier(rs.getObject("uuid", UUID.class)))
          .build();
    }
  }

}
