package fr.turgot.dao;

import java.util.List;

import fr.turgot.dao.model.Film;
import fr.turgot.utils.JPAUtil;
import jakarta.persistence.EntityManager;

public class FilmDAO {
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
                e.printStackTrace();
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