package fr.turgot.dao;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;

import org.junit.Test;

public class BilletDAOTest {
    @Test
    public void recognizesMysqlDuplicateKeyAsSeatConflict() {
        assertTrue(BilletDAO.isDuplicateSeatConstraint(
                new SQLException("Duplicate entry", "23000", 1062)));
    }

    @Test
    public void doesNotHideOtherDatabaseFailures() {
        assertFalse(BilletDAO.isDuplicateSeatConstraint(
                new SQLException("Connection failed", "08000", 0)));
    }
}
