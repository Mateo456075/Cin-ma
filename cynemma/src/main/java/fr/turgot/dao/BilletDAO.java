package fr.turgot.dao;

import java.util.List;

import fr.turgot.dao.model.Billet;
import fr.turgot.dao.model.Client;
import fr.turgot.dao.model.Reservation;
import fr.turgot.dao.model.Salle;
import fr.turgot.dao.model.Seance;
import fr.turgot.dao.model.Siege;
import fr.turgot.utils.JPAUtil;
import jakarta.persistence.EntityManager;

public class BilletDAO {
    public void create(Billet billet) {
        EntityManager em = JPAUtil.getEntityManagerFactory().createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(billet);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    public int getPlacesReservees(int seanceId) {
        EntityManager em = JPAUtil.getEntityManagerFactory().createEntityManager();
        try {
            Long total = em.createQuery(
                "SELECT COUNT(b) FROM Billet b WHERE b.seanceId = :seanceId",
                Long.class)
                .setParameter("seanceId", seanceId)
                .getSingleResult();
            return total.intValue();
        } finally {
            em.close();
        }
    }

    public boolean createReservation(int seanceId, int clientId, int nombrePlaces) {
        if (nombrePlaces < 1) {
            throw new IllegalArgumentException("Le nombre de places doit être positif.");
        }

        try (EntityManager em = JPAUtil.getEntityManagerFactory().createEntityManager()) {
            em.getTransaction().begin();
            try {
                Seance seance = em.find(Seance.class, seanceId);
                Client client = em.find(Client.class, clientId);
                if (seance == null || client == null) {
                    throw new IllegalArgumentException("Séance ou client introuvable.");
                }

                List<Siege> siegesDisponibles = em.createQuery(
                        "SELECT si FROM Siege si " +
                        "WHERE si.rangee.salle.id = :salleId " +
                        "AND si.id NOT IN (" +
                        "SELECT b.siege.id FROM Billet b WHERE b.seanceId = :seanceId) " +
                        "ORDER BY si.id",
                        Siege.class)
                    .setParameter("salleId", seance.getSalleId())
                    .setParameter("seanceId", seanceId)
                    .setMaxResults(nombrePlaces)
                    .getResultList();

                if (siegesDisponibles.size() < nombrePlaces) {
                    em.getTransaction().rollback();
                    return false;
                }

                Reservation reservation = new Reservation();
                reservation.setClient(client);
                for (Siege siege : siegesDisponibles) {
                    Billet billet = new Billet();
                    billet.setSeance(seance);
                    billet.setSiege(siege);
                    billet.setReservation(reservation);
                    reservation.getBillets().add(billet);
                }
                reservation.calculerMontantTotal();
                em.persist(reservation);
                em.getTransaction().commit();
                return true;
            } catch (RuntimeException e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                throw e;
            }
        }
    }

    public Salle getSalleBySeanceId(int seanceId) {
        EntityManager em = JPAUtil.getEntityManagerFactory().createEntityManager();
        try {
            return em.createQuery(
                "SELECT sa FROM Salle sa JOIN Seance s ON sa.id = s.salleId WHERE s.id = :seanceId",
                Salle.class)
                .setParameter("seanceId", seanceId)
                .getSingleResult();
        } finally {
            em.close();
        }
    }
}