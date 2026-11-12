package fr.turgot.dao.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "sieges")
public class Siege {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "numero", nullable = false)
    private int numero;

    @Column(name = "coordonnee_x")
    private Double coordonneeX;

    @Column(name = "coordonnee_z")
    private Double coordonneeZ;

    @Column(name = "angle_rotation")
    private Double angleRotation;

    @ManyToOne(optional = false)
    @JoinColumn(name = "rangee_id", nullable = false)
    private Rangee rangee;

    public Siege() {
    }

    public int getId()
    { return id; }
    public void setId(int id)
    { this.id = id; }

    public int getNumero()
    { return numero; }
    public void setNumero(int numero)
    { this.numero = numero; }

    public Double getCoordonneeX()
    { return coordonneeX; }
    public void setCoordonneeX(Double coordonneeX)
    { this.coordonneeX = coordonneeX; }

    public Double getCoordonneeZ()
    { return coordonneeZ; }
    public void setCoordonneeZ(Double coordonneeZ)
    { this.coordonneeZ = coordonneeZ; }

    public Double getAngleRotation()
    { return angleRotation; }
    public void setAngleRotation(Double angleRotation)
    { this.angleRotation = angleRotation; }
    
    public Rangee getRangee()
    { return rangee; }
    public void setRangee(Rangee rangee)
    { this.rangee = rangee; }
}
