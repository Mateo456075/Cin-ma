package fr.turgot.dao.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "seances")
public class Seance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "date_heure_debut", nullable = false)
    private LocalDateTime dateHeure;

    @Column(name = "film_id", nullable = false)
    private int filmId;

    @Column(name = "salle_id", nullable = false)
    private int salleId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "film_id", nullable = false, insertable = false, updatable = false)
    private Film film;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salle_id", nullable = false, insertable = false, updatable = false)
    private Salle salle;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_seance", nullable = false, length = 30)
    private TypeSeance typeSeance = TypeSeance.STANDARD;

    @Column(name = "date_heure_fin")
    private LocalDateTime dateHeureFin;

    @OneToMany(mappedBy = "seance")
    private List<Billet> billetsVendus = new ArrayList<>();

    public Seance() {
    }

    public Seance(LocalDateTime dateHeure, int filmId, int salleId) {
        this.dateHeure = dateHeure;
        this.filmId = filmId;
        this.salleId = salleId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public LocalDateTime getDateHeure() { return dateHeure; }
    public void setDateHeure(LocalDateTime dateHeure) { this.dateHeure = dateHeure; }

    public int getFilmId() { return film == null ? filmId : film.getId(); }

    public int getSalleId() { return salle == null ? salleId : salle.getId(); }

    public Film getFilm() { return film; }
    public void setFilm(Film film) {
        this.film = film;
        this.filmId = film == null ? 0 : film.getId();
    }

    public Salle getSalle() { return salle; }
    public void setSalle(Salle salle) {
        this.salle = salle;
        this.salleId = salle == null ? 0 : salle.getId();
    }

    public TypeSeance getTypeSeance() { return typeSeance; }
    public void setTypeSeance(TypeSeance typeSeance) { this.typeSeance = typeSeance; }

    public LocalDateTime getDateHeureDebut() { return dateHeure; }
    public void setDateHeureDebut(LocalDateTime dateHeureDebut) { this.dateHeure = dateHeureDebut; }

    public LocalDateTime getDateHeureFin() { return dateHeureFin; }
    public void setDateHeureFin(LocalDateTime dateHeureFin) { this.dateHeureFin = dateHeureFin; }

    public List<Billet> getBilletsVendus() { return billetsVendus; }
    public void setBilletsVendus(List<Billet> billetsVendus) { this.billetsVendus = billetsVendus; }

    public String getDate() { return dateHeure == null ? null : LocalDate.from(dateHeure).toString(); }
    public String getHoraire() { return dateHeure == null ? null : LocalTime.from(dateHeure).toString(); }
}
