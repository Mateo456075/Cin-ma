package fr.turgot.dao.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "billets")
public class Billet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "seance_id", nullable = false, insertable = false, updatable = false)
    private int seanceId;

    @Column(name = "tarif_applique", nullable = false, precision = 12, scale = 2)
    private BigDecimal tarifApplique = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "siege_id", nullable = false)
    private Siege siege;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seance_id", nullable = false)
    private Seance seance;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    public Billet() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSeanceId() { return seance == null ? seanceId : seance.getId(); }

    public BigDecimal getTarifApplique() { return tarifApplique; }
    public void setTarifApplique(BigDecimal tarifApplique) {
        this.tarifApplique = tarifApplique;
    }

    public Siege getSiege() { return siege; }
    public void setSiege(Siege siege) { this.siege = siege; }

    public Seance getSeance() { return seance; }
    public void setSeance(Seance seance) {
        this.seance = seance;
        this.seanceId = seance == null ? 0 : seance.getId();
    }

    public Reservation getReservation() { return reservation; }
    public void setReservation(Reservation reservation) { this.reservation = reservation; }
}
