package fr.turgot.dao.model;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "reservations")
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Billet> billets = new ArrayList<>();

    @Column(name = "date_reservation", nullable = false)
    private LocalDateTime dateReservation = LocalDateTime.now();

    @Column(name = "montant_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal montantTotal = BigDecimal.ZERO;

    public Reservation() {
    }

    public int getId()
    { return id; }
    public void setId(int id)
    { this.id = id; }

    public Client getClient()
    { return client; }
    public void setClient(Client client)
    { this.client = client; }

    public List<Billet> getBillets()
    { return billets; }
    public void setBillets(List<Billet> billets)
    { this.billets = billets; }

    public LocalDateTime getDateReservation()
    { return dateReservation; }
    public void setDateReservation(LocalDateTime dateReservation)
    { this.dateReservation = dateReservation; }
    
    public BigDecimal getMontantTotal()
    { return montantTotal; }
    public void setMontantTotal(BigDecimal montantTotal)
    { this.montantTotal = montantTotal; }

    public void calculerMontantTotal() {
        montantTotal = billets.stream()
                .map(Billet::getTarifApplique)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
