package fr.turgot.dao.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "configuration_salles")
public class ConfigurationSalle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "espacement_rangees")
    private Double espacementRangees;

    @Column(name = "largeur_allee")
    private Double largeurAllee;

    @Column(name = "nombre_balcons", nullable = false)
    private int nombreBalcons;

    @Enumerated(EnumType.STRING)
    @Column(name = "disposition", nullable = false, length = 30)
    private DispositionSieges disposition;

    public ConfigurationSalle() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Double getEspacementRangees() { return espacementRangees; }
    public void setEspacementRangees(Double espacementRangees) { this.espacementRangees = espacementRangees; }
    public Double getLargeurAllee() { return largeurAllee; }
    public void setLargeurAllee(Double largeurAllee) { this.largeurAllee = largeurAllee; }
    public int getNombreBalcons() { return nombreBalcons; }
    public void setNombreBalcons(int nombreBalcons) { this.nombreBalcons = nombreBalcons; }
    public DispositionSieges getDisposition() { return disposition; }
    public void setDisposition(DispositionSieges disposition) { this.disposition = disposition; }
}
