package fr.turgot.dao.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.LocalDateTime;

import org.junit.Test;

public class SeanceTest {
    @Test
    public void exposesDateAndTimeFromTheStoredDateTime() {
        Seance seance = new Seance();
        seance.setDateHeure(LocalDateTime.of(2026, 11, 18, 19, 45));

        assertEquals("2026-11-18", seance.getDate());
        assertEquals("19:45", seance.getHoraire());
    }

    @Test
    public void returnsNullDateAndTimeWhenNoDateTimeIsSet() {
        Seance seance = new Seance();

        assertNull(seance.getDate());
        assertNull(seance.getHoraire());
    }
}
