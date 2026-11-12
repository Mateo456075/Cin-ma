package fr.turgot.dao.model;

import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "salles")
public class Salle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;
    
    @Column(name = "nom", nullable = false, length = 255)
    private String nom;

    @Column(name = "capacite", nullable = false)
    private int capacite;

    @OneToOne(optional = false)
    @JoinColumn(name = "configuration_id", nullable = false, unique = true)
    private ConfigurationSalle configuration;

    @OneToMany(mappedBy = "salle", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Rangee> rangees = new ArrayList<>();

    @OneToMany(mappedBy = "salle")
    private List<Seance> seances = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "type_salle", nullable = false, length = 30)
    private TypeSalle typeSalle;

    public Salle() {
    }

    public Salle(String nom, int capacite) {
        this.nom = nom;
        this.capacite = capacite;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public int getCapacite() { return capacite; }
    public void setCapacite(int capacite) { this.capacite = capacite; }
    public ConfigurationSalle getConfiguration() { return configuration; }
    public void setConfiguration(ConfigurationSalle configuration) { this.configuration = configuration; }
    public List<Rangee> getRangees() { return rangees; }
    public void setRangees(List<Rangee> rangees) { this.rangees = rangees; }
    public List<Seance> getSeances() { return seances; }
    public void setSeances(List<Seance> seances) { this.seances = seances; }
    public TypeSalle getTypeSalle() { return typeSalle; }
    public void setTypeSalle(TypeSalle typeSalle) { this.typeSalle = typeSalle; }

    public int calculerCapaciteReelle() {
        return rangees.stream().mapToInt(Rangee::getNombreSieges).sum();
    }

}
