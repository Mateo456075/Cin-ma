package fr.turgot.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import fr.turgot.dao.model.ConfigurationSalle;
import fr.turgot.dao.model.DispositionSieges;
import fr.turgot.dao.model.Rangee;
import fr.turgot.dao.model.Siege;

class SeatLayoutCalculatorTest {
    @Test
    void positionsEveryDispositionWithoutNullCoordinates() {
        for (DispositionSieges disposition : DispositionSieges.values()) {
            List<Rangee> rangees = createRows(disposition, 3, 12, 0);
            for (Rangee rangee : rangees) {
                assertNotNull(rangee.getElevationY());
                for (Siege siege : rangee.getSieges()) {
                    assertNotNull(siege.getCoordonneeX());
                    assertNotNull(siege.getCoordonneeZ());
                    assertNotNull(siege.getAngleRotation());
                }
            }
        }
    }

    @Test
    void distributesBalconiesOnlyForUpperRowsAndKeepsImaxAtZero() {
        List<Rangee> rectangle = createRows(DispositionSieges.RECTANGLE, 4, 8, 2);
        assertEquals(0.0, rectangle.get(0).getElevationY());
        assertNotEquals(0.0, rectangle.get(2).getElevationY());
        assertNotEquals(0.0, rectangle.get(3).getElevationY());

        List<Rangee> imax = createRows(DispositionSieges.IMAX, 3, 8, 0);
        for (Rangee rangee : imax) {
            assertEquals(0.0, rangee.getElevationY());
        }
    }

    @Test
    void rectangleIsReproducibleAndHasDistinctSeatPositions() {
        List<Rangee> first = createRows(DispositionSieges.RECTANGLE, 2, 10, 0);
        List<Rangee> second = createRows(DispositionSieges.RECTANGLE, 2, 10, 0);

        for (int row = 0; row < first.size(); row++) {
            for (int seat = 0; seat < first.get(row).getSieges().size(); seat++) {
                Siege a = first.get(row).getSieges().get(seat);
                Siege b = second.get(row).getSieges().get(seat);
                assertEquals(a.getCoordonneeX(), b.getCoordonneeX());
                assertEquals(a.getCoordonneeZ(), b.getCoordonneeZ());
            }
        }

        for (Rangee rangee : first) {
            for (Siege siege : rangee.getSieges()) {
                assertFalse(Double.isNaN(siege.getCoordonneeX()));
                assertFalse(Double.isNaN(siege.getCoordonneeZ()));
            }
        }
    }

    private List<Rangee> createRows(DispositionSieges disposition, int rowCount,
            int seatsPerRow, int balconyCount) {
        ConfigurationSalle configuration = new ConfigurationSalle();
        configuration.setDisposition(disposition);
        configuration.setNombreBalcons(disposition == DispositionSieges.IMAX ? 0 : balconyCount);
        configuration.setEspacementRangees(1.2);
        configuration.setLargeurAllee(1.0);

        List<Rangee> rangees = new ArrayList<>();
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            Rangee rangee = new Rangee();
            rangee.setNomRangee("R" + (rowIndex + 1));
            rangee.setNombreSieges(seatsPerRow);
            List<Siege> seats = new ArrayList<>();
            for (int seatIndex = 0; seatIndex < seatsPerRow; seatIndex++) {
                Siege siege = new Siege();
                siege.setNumero(seatIndex + 1);
                siege.setRangee(rangee);
                seats.add(siege);
            }
            rangee.setSieges(seats);
            rangees.add(rangee);
        }
        SeatLayoutCalculator.applyLayout(rangees, configuration);
        return rangees;
    }
}
