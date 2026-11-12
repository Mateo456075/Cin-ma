package fr.turgot.dao.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "films")
public class Film {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "titre", nullable = false, length = 255)
    private String titre;

    @Column(name = "realisateur", nullable = false, length = 255)
    private String realisateur;

    @Column(name = "annee", nullable = false)
    private int annee;

    @Column(name = "genre", nullable = false, length = 100)
    private String genre;

    @Column(name = "affiche", nullable = false, length = 255)
    private String affiche;

    @Column(name = "duree_minutes", nullable = false)
    private int dureeMinutes;

    @OneToMany(mappedBy = "film")
    private List<Seance> seances = new ArrayList<>();

    public Film() {
    }

    public Film(String titre, String realisateur, int annee, String genre, String affiche, int temps) {
        this.titre = titre;
        this.realisateur = realisateur;
        this.annee = annee;
        this.genre = genre;
        this.affiche = affiche;
        this.dureeMinutes = temps;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getRealisateur() { return realisateur; }
    public void setRealisateur(String realisateur) { this.realisateur = realisateur; }

    public int getAnnee() { return annee; }
    public void setAnnee(int annee) { this.annee = annee; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public String getAffiche() { return affiche; }
    public void setAffiche(String affiche) { this.affiche = affiche; }

    public int getDureeMinutes() { return dureeMinutes; }
    public void setDureeMinutes(int dureeMinutes) { this.dureeMinutes = dureeMinutes; }

    public int getTemps() { return dureeMinutes; }
    public void setTemps(int temps) { this.dureeMinutes = temps; }

    public List<Seance> getSeances() { return seances; }
    public void setSeances(List<Seance> seances) { this.seances = seances; }

    public String getDureeFormatee() {
        return String.format("%dh%02d", dureeMinutes / 60, dureeMinutes % 60);
    }
}
