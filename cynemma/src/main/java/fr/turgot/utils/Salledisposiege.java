package fr.turgot.utils;
import java.io.IOException;
import java.io.PrintWriter;

public class Salledisposiege {

    public static void main(String[] args) {
        int[] capacites = {500, 375, 375, 175, 175, 150, 150, 100, 90, 75};
        
        try (PrintWriter writer = new PrintWriter("insert_places.sql", "UTF-8")) {
            writer.println("INSERT INTO places (salle_id, rangee, numero, coord_x, coord_z, angle_y) VALUES");
            
            boolean first = true;

            for (int i = 0; i < capacites.length; i++) {
                int idSalle = i + 1;
                int capacite = capacites[i];

                if (idSalle == 1) {
                    genererSalleArc(writer, idSalle, capacite, first);
                } else {
                    genererSalleRectangulaire(writer, idSalle, capacite, first);
                }
                first = false; // Les virgules SQL
            }
            writer.println(";");
            System.out.println("Fichier SQL généré avec succès !");
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- MODELE 1 : SALLE IMAX EN ARC DE CERCLE ---
    private static void genererSalleArc(PrintWriter writer, int idSalle, int capacite, boolean first) {
        int placesParRangee = 25;
        
        for (int i = 0; i < capacite; i++) {
            int indexRangee = i / placesParRangee;
            int numeroPlace = (i % placesParRangee) + 1;
            char nomRangee = (char) ('A' + indexRangee);
            
            double rayonBase = 12.0;
            double ecartRangee = 1.5;
            double rayonActuel = rayonBase + (indexRangee * ecartRangee);
            
            double angleMax = Math.toRadians(35); 
            double angleStep = (angleMax * 2) / (placesParRangee - 1);
            double angle = -angleMax + ((numeroPlace - 1) * angleStep);
            
            double x = rayonActuel * Math.sin(angle);
            double z = rayonActuel * Math.cos(angle);
            double angleY = angle;

            ecrireLigneSQL(writer, idSalle, nomRangee, numeroPlace, x, z, angleY);
        }
    }

    // --- MODELE 2 : SALLE RECTANGULAIRE AVEC ALLÉES (Ton schéma) ---
    private static void genererSalleRectangulaire(PrintWriter writer, int idSalle, int capacite, boolean first) {
        int placesParRangee = 20;
        int placesBlocGauche = 5;
        int placesBlocCentre = 10;
        
        double largeurSiege = 1.2;
        double profondeurRangee = 1.5;
        double largeurAllee = 2.5;
        double largeurAlleeHorizontale = 3.0;
        
        for (int i = 0; i < capacite; i++) {
            int indexRangee = i / placesParRangee;
            int numeroPlace = (i % placesParRangee) + 1;
            char nomRangee = (char) ('A' + indexRangee);

            double x = 0;
            
            if (numeroPlace <= placesBlocGauche) {
                x = numeroPlace * largeurSiege;
            } else if (numeroPlace <= (placesBlocGauche + placesBlocCentre)) {
                x = (numeroPlace * largeurSiege) + largeurAllee;
            } else {
                x = (numeroPlace * largeurSiege) + (largeurAllee * 2);
            }
            
            x = x - (((placesParRangee * largeurSiege) + (largeurAllee * 2)) / 2);

            double z = indexRangee * profondeurRangee;
            
            if (indexRangee >= 3) {
                z += largeurAlleeHorizontale;
            }
            ecrireLigneSQL(writer, idSalle, nomRangee, numeroPlace, x, z, 0.0);
        }
    }

    private static void ecrireLigneSQL(PrintWriter writer, int idSalle, char rangee, int numero, double x, double z, double angleY) {
        String ligne = String.format("('%d', '%c', '%d', '%.2f', '%.2f', '%.2f')",
                                    idSalle, rangee, numero, x, z, angleY)
                                    .replace(",", ".");
        writer.print(",\n" + ligne);
    }
}
