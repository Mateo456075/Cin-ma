package fr.turgot.dao;
import java.util.List;

import org.mindrot.jbcrypt.BCrypt;

import fr.turgot.dao.model.Client;
import fr.turgot.dao.model.Utilisateur;
import fr.turgot.utils.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

public class UserDAO {
    public Utilisateur authenticate(String username, String plainPassword) {

        String jpql = "SELECT u FROM Utilisateur u WHERE u.login = :u";

        try (EntityManager em = JPAUtil.getEntityManagerFactory().createEntityManager()) {

            TypedQuery<Utilisateur> query = em.createQuery(jpql, Utilisateur.class);
            query.setParameter("u", username);
            query.setMaxResults(1);

            Utilisateur user = query.getSingleResult();
            try {
                if (BCrypt.checkpw(plainPassword, user.getMotDePasse())) {
                    return user;
                }
            } catch (IllegalArgumentException e) {
                if (plainPassword.equals(user.getMotDePasse())) {
                    return user;
                }
            }
            return null;

        } catch (NoResultException e) {
            return null;
        }
    }


    public Utilisateur create(String username, String password, String email, String login) {
        try (EntityManager emin = JPAUtil.getEntityManagerFactory().createEntityManager()) {
            try {
                emin.getTransaction().begin();

                Utilisateur user = new Client();
                user.setLogin(login);
                user.setNom(username);
                user.setMotDePasse(BCrypt.hashpw(password, BCrypt.gensalt()));
                user.setEmail(email); // Set the email
                emin.persist(user);
                emin.getTransaction().commit();
                return user;
            } catch (Exception e) {
                if (emin.getTransaction().isActive()) {
                    emin.getTransaction().rollback();
                }
                return null;
            }
        }
    }

    public List<Utilisateur> getAllUsers() {
        try (EntityManager em = JPAUtil.getEntityManagerFactory().createEntityManager()) {
            return em.createQuery("SELECT u FROM Utilisateur u", Utilisateur.class).getResultList();
        }
    }

    public boolean updateRole(String login, String newRole) {
        if (newRole == null) {
            return false;
        }
        String discriminator = switch (newRole.toUpperCase(java.util.Locale.ROOT)) {
            case "CLIENT" -> "CLIENT";
            case "EMPLOYER", "EMPLOYE" -> "EMPLOYE";
            case "ADMIN", "ADMINISTRATEUR" -> "ADMINISTRATEUR";
            default -> null;
        };
        if (discriminator == null) {
            return false;
        }

        try (EntityManager em = JPAUtil.getEntityManagerFactory().createEntityManager()) {
            em.getTransaction().begin();
            int updated = em.createNativeQuery(
                    "UPDATE utilisateurs SET type_utilisateur = :type WHERE login = :login")
                    .setParameter("type", discriminator)
                    .setParameter("login", login)
                    .executeUpdate();
            em.getTransaction().commit();
            return updated == 1;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean deleteUser(String login) {
        try (EntityManager em = JPAUtil.getEntityManagerFactory().createEntityManager()) {
            em.getTransaction().begin();
            TypedQuery<Utilisateur> query = em.createQuery("SELECT u FROM Utilisateur u WHERE u.login = :u", Utilisateur.class);
            query.setParameter("u", login);
            Utilisateur user = query.getSingleResult();
            em.remove(user);
            em.getTransaction().commit();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean updatePassword(String login, String newPassword) {
        try (EntityManager em = JPAUtil.getEntityManagerFactory().createEntityManager()) {
            em.getTransaction().begin();
            TypedQuery<Utilisateur> query = em.createQuery("SELECT u FROM Utilisateur u WHERE u.login = :u", Utilisateur.class);
            query.setParameter("u", login);
            Utilisateur user = query.getSingleResult();
            // Hachage du nouveau mot de passe avant de le sauvegarder
            user.setMotDePasse(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
            em.getTransaction().commit();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
