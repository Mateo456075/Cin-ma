package fr.turgot.dao.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "cartes_fidelite")
public class CarteFidelite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "numero_carte", nullable = false, unique = true, length = 50)
    private String numeroCarte;

    @Column(name = "points", nullable = false)
    private int points;

    @OneToOne
    @JoinColumn(name = "abonne_id", nullable = false, unique = true)
    private Client abonne;

    public CarteFidelite() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNumeroCarte() { return numeroCarte; }
    public void setNumeroCarte(String numeroCarte) { this.numeroCarte = numeroCarte; }
    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }
    public Client getAbonne() { return abonne; }
    public void setAbonne(Client abonne) { this.abonne = abonne; }

    public void cumulerPoints(double montantAchat) {
        if (montantAchat < 0) {
            throw new IllegalArgumentException("Le montant d'achat ne peut pas être négatif.");
        }
        points += (int) montantAchat;
    }

    public boolean utiliserPoints(int pointsRequis) {
        if (pointsRequis < 0) {
            throw new IllegalArgumentException("Le nombre de points ne peut pas être négatif.");
        }
        if (points < pointsRequis) {
            return false;
        }
        points -= pointsRequis;
        return true;
    }
}
