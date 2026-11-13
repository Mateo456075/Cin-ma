package fr.turgot.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Optional;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

public class TmdbCacheDAO {

    private static final String DATA_SOURCE_NAME = "java:comp/env/jdbc/cynema";

    public Optional<CacheEntry> findByFilmTitle(String filmTitle) throws SQLException {
        String sql = "SELECT tmdb_id, trailer_key FROM tmdb_cache WHERE film_title = ?";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, filmTitle);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }

                int tmdbIdValue = result.getInt("tmdb_id");
                Integer tmdbId = result.wasNull() ? null : tmdbIdValue;
                return Optional.of(new CacheEntry(tmdbId, result.getString("trailer_key")));
            }
        }
    }

    public void save(String filmTitle, Integer tmdbId, String trailerKey) throws SQLException {
        String sql = """
                INSERT INTO tmdb_cache (film_title, tmdb_id, trailer_key)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    tmdb_id = VALUES(tmdb_id),
                    trailer_key = VALUES(trailer_key),
                    cached_at = CURRENT_TIMESTAMP
                """;

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, filmTitle);
            if (tmdbId == null) {
                statement.setNull(2, Types.INTEGER);
            } else {
                statement.setInt(2, tmdbId);
            }
            statement.setString(3, trailerKey);
            statement.executeUpdate();
        }
    }

    private Connection getConnection() throws SQLException {
        try {
            DataSource dataSource = InitialContext.doLookup(DATA_SOURCE_NAME);
            return dataSource.getConnection();
        } catch (NamingException e) {
            throw new SQLException(
                    "La DataSource JNDI " + DATA_SOURCE_NAME + " n'est pas configurée.", e);
        }
    }

    public record CacheEntry(Integer tmdbId, String trailerKey) {
    }
}
