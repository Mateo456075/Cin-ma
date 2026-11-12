package fr.turgot.dao.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("EMPLOYE")
public class Employe extends Utilisateur {
    public Employe() {
    }
}
