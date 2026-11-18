package fr.turgot.dao;

import java.util.List;

import fr.turgot.dao.model.Siege;
import fr.turgot.utils.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

public class SiegeDAO {

    /**
     * Persiste un siège (la rangée doit déjà exister en base).
     */
    public void create(Siege siege) {
        EntityManager em = JPAUtil.getEntityManagerFactory().createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(siege);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    /**
     * Retourne tous les sièges d'une rangée donnée.
     */
    public List<Siege> getByIdRangee(int rangeeId) {
        EntityManager em = JPAUtil.getEntityManagerFactory().createEntityManager();
        try {
            TypedQuery<Siege> query = em.createQuery(
                "SELECT s FROM Siege s WHERE s.rangee.id = :rangeeId ORDER BY s.numero",
                Siege.class);
            query.setParameter("rangeeId", rangeeId);
            return query.getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Retourne tous les sièges d'une salle entière (via ses rangées).
     */
    public List<Siege> getByIdSalle(int salleId) {
        EntityManager em = JPAUtil.getEntityManagerFactory().createEntityManager();
        try {
            TypedQuery<Siege> query = em.createQuery(
                "SELECT s FROM Siege s WHERE s.rangee.salle.id = :salleId ORDER BY s.rangee.nomRangee, s.numero",
                Siege.class);
            query.setParameter("salleId", salleId);
            return query.getResultList();
        } finally {
            em.close();
        }
    }
}
