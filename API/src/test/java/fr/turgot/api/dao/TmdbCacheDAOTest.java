package fr.turgot.api.dao;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;

import org.junit.jupiter.api.Test;

class TmdbCacheDAOTest {
    private final TmdbCacheDAO dao = new TmdbCacheDAO();

    @Test
    void reportsMissingJndiDatasourceForRead() {
        SQLException exception = assertThrows(SQLException.class,
                () -> dao.findByFilmTitle("Interstellar"));
        assertTrue(exception.getMessage().contains("DataSource JNDI"));
    }

    @Test
    void reportsMissingJndiDatasourceForWriteAndHealthCheck() {
        SQLException write = assertThrows(SQLException.class,
                () -> dao.save("Interstellar", 157336, "zSWdZVtXT7E"));
        assertTrue(write.getMessage().contains("DataSource JNDI"));

        SQLException health = assertThrows(SQLException.class, dao::checkConnection);
        assertTrue(health.getMessage().contains("DataSource JNDI"));
    }
}
