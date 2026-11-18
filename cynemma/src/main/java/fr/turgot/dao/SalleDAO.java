package fr.turgot.dao;

import java.util.List;

import fr.turgot.dao.model.ConfigurationSalle;
import fr.turgot.dao.model.Salle;
import fr.turgot.utils.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

public class SalleDAO {

    /**
     * Crée une salle simple (sans configuration — ancienne méthode conservée).
     */
    public void create(Salle salle) {
        EntityManager em = JPAUtil.getEntityManagerFactory().createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(salle);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    /**
     * Crée une salle avec sa ConfigurationSalle dans la même transaction.
     * La configuration est persistée en premier, puis liée à la salle.
     */
    public void createAvecConfiguration(Salle salle, ConfigurationSalle config) {
        EntityManager em = JPAUtil.getEntityManagerFactory().createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(config);          // 1. on insère d'abord la config
            salle.setConfiguration(config);
            em.persist(salle);           // 2. puis la salle (qui référence la config)
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    public List<Salle> getAllSalles() {
        EntityManager em = JPAUtil.getEntityManagerFactory().createEntityManager();
        try {
            TypedQuery<Salle> query = em.createQuery("SELECT s FROM Salle s", Salle.class);
            return query.getResultList();
        } finally {
            em.close();
        }
    }

    public Salle findById(int id) {
        EntityManager em = JPAUtil.getEntityManagerFactory().createEntityManager();
        try {
            return em.find(Salle.class, id);
        } finally {
            em.close();
        }
    }
}
