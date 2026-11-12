package fr.turgot.dao;

import java.util.List;

import fr.turgot.dao.model.Salle;
import fr.turgot.utils.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

public class SalleDAO {
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

    public List<Salle> getAllSalles() {
        EntityManager em = JPAUtil.getEntityManagerFactory().createEntityManager();
        try {
            TypedQuery<Salle> query = em.createQuery("SELECT s FROM Salle s", Salle.class);
            return query.getResultList();
        } finally {
            em.close();
        }
    }
    
}