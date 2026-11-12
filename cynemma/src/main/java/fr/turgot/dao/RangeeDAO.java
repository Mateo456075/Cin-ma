package fr.turgot.dao;

import java.util.List;

import fr.turgot.dao.model.Rangee;
import fr.turgot.utils.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

public class RangeeDAO {

    /**
     * Persiste une rangée (la salle doit déjà exister en base).
     */
    public void create(Rangee rangee) {
        EntityManager em = JPAUtil.getEntityManagerFactory().createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(rangee);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    /**
     * Retourne toutes les rangées d'une salle donnée.
     */
    public List<Rangee> getByIdSalle(int salleId) {
        EntityManager em = JPAUtil.getEntityManagerFactory().createEntityManager();
        try {
            TypedQuery<Rangee> query = em.createQuery(
                "SELECT r FROM Rangee r WHERE r.salle.id = :salleId ORDER BY r.nomRangee",
                Rangee.class);
            query.setParameter("salleId", salleId);
            return query.getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Trouve une rangée par son id.
     */
    public Rangee findById(int id) {
        EntityManager em = JPAUtil.getEntityManagerFactory().createEntityManager();
        try {
            return em.find(Rangee.class, id);
        } finally {
            em.close();
        }
    }
}
