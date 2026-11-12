package fr.turgot.dao;

import java.time.LocalDateTime;
import java.util.List;

import fr.turgot.dao.model.Film;
import fr.turgot.dao.model.Seance;
import fr.turgot.utils.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

public class SeanceDAO {
    public void create(Seance seance) {
        EntityManager em = JPAUtil.getEntityManagerFactory().createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(seance);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    public List<Seance> getAllSeances() {
        EntityManager em = JPAUtil.getEntityManagerFactory().createEntityManager();
        try {
            return em.createQuery("SELECT s FROM Seance s ORDER BY s.dateHeure", Seance.class).getResultList();
        } finally {
            em.close();
        }
    }

    public boolean isSalleOccupee(int salleId, LocalDateTime nouveauDebut, int filmId) {
        EntityManager em = JPAUtil.getEntityManagerFactory().createEntityManager();
        try {
            Film film = em.find(Film.class, filmId);
            int tempsOccupation = film.getTemps() + 20;
            LocalDateTime nouvelleFin = nouveauDebut.plusMinutes(tempsOccupation);

            String sql = "SELECT COUNT(s.id) FROM seances s " +
                        "JOIN films f ON s.film_id = f.id " +
                        "WHERE s.salle_id = :salleId " +
                        "AND s.date_heure_debut < :nouvelleFin " +
                        "AND COALESCE(s.date_heure_fin, " +
                        "s.date_heure_debut + (f.duree_minutes + 20) * INTERVAL '1 minute') > :nouveauDebut";

            Query query = em.createNativeQuery(sql);
            query.setParameter("salleId", salleId);
            query.setParameter("nouvelleFin", java.sql.Timestamp.valueOf(nouvelleFin));
            query.setParameter("nouveauDebut", java.sql.Timestamp.valueOf(nouveauDebut));

            long nbConflits = ((Number) query.getSingleResult()).longValue();
            return nbConflits > 0;
        } finally {
            em.close();
        }
    }
    public List<Seance> getSeancesByFilmId(int filmId) {
    EntityManager em = JPAUtil.getEntityManagerFactory().createEntityManager();
        try {
            return em.createQuery(
                "SELECT s FROM Seance s WHERE s.filmId = :filmId ORDER BY s.dateHeure",
                Seance.class)
                .setParameter("filmId", filmId)
                .getResultList();
        } finally {
            em.close();
        }
    }
    public Seance getSeanceById(int id) {
        EntityManager em = JPAUtil.getEntityManagerFactory().createEntityManager();
        try {
            return em.find(Seance.class, id);
        } finally {
            em.close();
        }
    }
}