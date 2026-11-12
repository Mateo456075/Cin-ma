package fr.turgot.dao.model;

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
@Table(name = "rangees")
public class Rangee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "nom_rangee", nullable = false, length = 20)
    private String nomRangee;

    @Column(name = "nombre_sieges", nullable = false)
    private int nombreSieges;

    @Column(name = "elevation_y")
    private Double elevationY;

    @ManyToOne(optional = false)
    @JoinColumn(name = "salle_id", nullable = false)
    private Salle salle;

    @OneToMany(mappedBy = "rangee", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Siege> sieges = new ArrayList<>();

    public Rangee() {
    }

    public int getId()
    { return id; }
    public void setId(int id)
    { this.id = id; }

    public String getNomRangee()
    { return nomRangee; }
    public void setNomRangee(String nomRangee)
    { this.nomRangee = nomRangee; }

    public int getNombreSieges()
    { return nombreSieges; }
    public void setNombreSieges(int nombreSieges)
    { this.nombreSieges = nombreSieges; }

    public Double getElevationY()
    { return elevationY; }
    public void setElevationY(Double elevationY)
    { this.elevationY = elevationY; }

    public Salle getSalle()
    { return salle; }
    public void setSalle(Salle salle)
    { this.salle = salle; }
    
    public List<Siege> getSieges()
    { return sieges; }
    public void setSieges(List<Siege> sieges)
    { this.sieges = sieges; }
}
