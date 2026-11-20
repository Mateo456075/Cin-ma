package fr.turgot.dao;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.turgot.dao.model.Film;
import fr.turgot.utils.JPAUtil;
import jakarta.persistence.EntityManager;

public class FilmDAO {
    private static final Logger LOGGER = Logger.getLogger(FilmDAO.class.getName());

    public Film create(Film film) {
        try (EntityManager em = JPAUtil.getEntityManagerFactory().createEntityManager()) {
            try {
                em.getTransaction().begin();
                em.persist(film);
                em.getTransaction().commit();
                return film;
            } catch (Exception e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                LOGGER.log(Level.SEVERE, "Impossible de créer le film.", e);
                return null;
            }
        }
    }

    public List<Film> getAllFilms() {
        try (EntityManager em = JPAUtil.getEntityManagerFactory().createEntityManager()) {
            return em.createQuery("SELECT f FROM Film f", Film.class).getResultList();
        }
    }

    public Film getFilmById(int id) {
        try (EntityManager em = JPAUtil.getEntityManagerFactory().createEntityManager()) {
            return em.find(Film.class, id);
        }
    }

    public Film getFilmByTmdbId(int tmdbId) {
        try (EntityManager em = JPAUtil.getEntityManagerFactory().createEntityManager()) {
            List<Film> films = em.createQuery(
                    "SELECT f FROM Film f WHERE f.tmdbId = :tmdbId", Film.class)
                    .setParameter("tmdbId", tmdbId)
                    .setMaxResults(1)
                    .getResultList();
            return films.isEmpty() ? null : films.get(0);
        }
    }

    public boolean isAfficheUsedByAnotherFilm(String affiche) {
        try (EntityManager em = JPAUtil.getEntityManagerFactory().createEntityManager()) {
            Long count = em.createQuery(
                    "SELECT COUNT(f) FROM Film f WHERE f.affiche = :affiche",
                    Long.class)
                .setParameter("affiche", affiche)
                .getSingleResult();
            return count > 0;
        }
    }

    public boolean update(Film film) {
        try (EntityManager em = JPAUtil.getEntityManagerFactory().createEntityManager()) {
            try {
                em.getTransaction().begin();
                em.merge(film);
                em.getTransaction().commit();
                return true;
            } catch (Exception e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                e.printStackTrace();
                return false;
            }
        }
    }

    public boolean delete(int id) {
        try (EntityManager em = JPAUtil.getEntityManagerFactory().createEntityManager()) {
            try {
                em.getTransaction().begin();

                Long count = em.createQuery(
                        "SELECT COUNT(s) FROM Seance s WHERE s.filmId = :filmId",
                        Long.class)
                    .setParameter("filmId", id)
                    .getSingleResult();

                if (count > 0) {
                    em.getTransaction().rollback();
                    return false;
                }

                Film film = em.find(Film.class, id);
                if (film != null) {
                    em.remove(film);
                }

                em.getTransaction().commit();
                return true;
            } catch (Exception e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                e.printStackTrace();
                return false;
            }
        }
    }


}