package fr.turgot.dao.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

@Entity
@Table(name = "utilisateurs")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type_utilisateur", length = 20)
public abstract class Utilisateur {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "nom", length = 70)
    private String nom;

    @Column(name = "email", nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "mot_de_passe", nullable = false, length = 200)
    private String motDePasse;

    @Column(name = "login", nullable = false, unique = true, length = 50)
    private String login;

    protected Utilisateur() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getTypeUtilisateur() {
        if (this instanceof Employe) {
            return "EMPLOYE";
        }
        if (this instanceof Administrateur) {
            return "ADMINISTRATEUR";
        }
        return "CLIENT";
    }
}
