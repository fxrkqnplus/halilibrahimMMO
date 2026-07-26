package com.halilibrahim.halilibrahimmmo.skills;

import com.halilibrahim.halilibrahimmmo.database.DatabaseService;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** JDBC repository for durable player skill progression. */
public final class SkillRepository {

    private final DatabaseService database;

    public SkillRepository(DatabaseService database) {
        this.database = database;
    }

    CompletableFuture<Map<String, SkillProgress>> findAll(UUID playerId) {
        return database.execute(connection -> {
            Map<String, SkillProgress> found = new LinkedHashMap<>();
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT skill_id, skill_level, skill_experience
                    FROM hmmo_player_skills WHERE player_id = ?
                    """)) {
                statement.setString(1, playerId.toString());
                try (ResultSet results = statement.executeQuery()) {
                    while (results.next()) {
                        String skillId = results.getString("skill_id");
                        found.put(skillId, new SkillProgress(playerId, skillId,
                                results.getInt("skill_level"), results.getLong("skill_experience")));
                    }
                }
            }
            return Map.copyOf(found);
        });
    }

    CompletableFuture<Void> save(SkillProgress progress) {
        return database.executeVoid(connection -> {
            try (PreparedStatement update = connection.prepareStatement("""
                    UPDATE hmmo_player_skills
                    SET skill_level = ?, skill_experience = ?, updated_at = %s
                    WHERE player_id = ? AND skill_id = ?
                    """.formatted(database.dialect().currentTimestampExpression()))) {
                update.setInt(1, progress.level());
                update.setLong(2, progress.experience());
                update.setString(3, progress.playerId().toString());
                update.setString(4, progress.skillId());
                if (update.executeUpdate() != 0) {
                    return;
                }
            }
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO hmmo_player_skills (player_id, skill_id, skill_level, skill_experience)
                    VALUES (?, ?, ?, ?)
                    """)) {
                insert.setString(1, progress.playerId().toString());
                insert.setString(2, progress.skillId());
                insert.setInt(3, progress.level());
                insert.setLong(4, progress.experience());
                insert.executeUpdate();
            }
        });
    }
}
