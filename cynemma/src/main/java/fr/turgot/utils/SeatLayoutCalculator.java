package fr.turgot.utils;

import java.util.List;

import fr.turgot.dao.model.ConfigurationSalle;
import fr.turgot.dao.model.DispositionSieges;
import fr.turgot.dao.model.Rangee;
import fr.turgot.dao.model.Siege;

public final class SeatLayoutCalculator {
    private SeatLayoutCalculator() {
    }

    public static void applyLayout(List<Rangee> rangees, ConfigurationSalle configuration) {
        if (rangees == null || configuration == null || configuration.getDisposition() == null) {
            throw new IllegalArgumentException("La disposition et les rangées sont obligatoires.");
        }

        double rowSpacing = positiveOrDefault(configuration.getEspacementRangees(), 1.2);
        double aisleWidth = positiveOrDefault(configuration.getLargeurAllee(), 1.0);
        int balconyCount = Math.max(0, configuration.getNombreBalcons());
        if (balconyCount > rangees.size()) {
            balconyCount = rangees.size();
        }
        DispositionSieges disposition = configuration.getDisposition();
        int rowCount = rangees.size();

        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            Rangee rangee = rangees.get(rowIndex);
            int rowFromScreen = rowCount - rowIndex;
            boolean balcony = balconyCount > 0 && rowIndex >= rowCount - balconyCount;
            int balconyIndex = balcony ? rowIndex - (rowCount - balconyCount) : -1;
            rangee.setElevationY(balcony ? 2.5 * (balconyIndex + 1) : 0.0);

            List<Siege> sieges = rangee.getSieges();
            if (sieges == null || sieges.isEmpty()) {
                continue;
            }
            int seatCount = sieges.size();
            double seatSpacing = disposition == DispositionSieges.IMAX ? 0.95 : 0.8;

            for (int seatIndex = 0; seatIndex < seatCount; seatIndex++) {
                Siege siege = sieges.get(seatIndex);
                double x;
                double z;
                double angle;

                if (disposition == DispositionSieges.ARC_DE_CERCLE) {
                    double radius = 18.0 + rowIndex * rowSpacing;
                    double arcHalfWidth = Math.min(1.05, seatCount * seatSpacing / (2.0 * radius));
                    double angleFromCenter = -arcHalfWidth
                            + (seatCount == 1 ? 0.0 : 2.0 * arcHalfWidth * seatIndex / (seatCount - 1));
                    x = radius * Math.sin(angleFromCenter);
                    z = -radius * Math.cos(angleFromCenter);
                    angle = Math.toDegrees(-angleFromCenter);
                } else if (disposition == DispositionSieges.SCREEN_X) {
                    double spread = seatCount * seatSpacing;
                    x = (seatCount == 1 ? 0.0 : -spread / 2.0
                            + spread * seatIndex / (seatCount - 1));
                    z = -rowIndex * rowSpacing
                            - Math.abs(x) * 0.035;
                    angle = Math.toDegrees(Math.atan2(-x, 12.0));
                } else {
                    double spread = seatCount * seatSpacing;
                    x = (seatCount == 1 ? 0.0 : -spread / 2.0
                            + spread * seatIndex / (seatCount - 1));
                    z = -rowFromScreen * rowSpacing;
                    angle = 0.0;
                }

                if (seatIndex == seatCount / 2 && seatCount > 8) {
                    x += seatIndex % 2 == 0 ? aisleWidth / 2.0 : -aisleWidth / 2.0;
                }

                siege.setCoordonneeX(round(x));
                siege.setCoordonneeZ(round(z));
                siege.setAngleRotation(round(angle));
            }
        }
    }

    private static double positiveOrDefault(Double value, double fallback) {
        return value != null && value > 0.0 ? value : fallback;
    }

    private static double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
