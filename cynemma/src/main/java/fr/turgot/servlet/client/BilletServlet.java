package fr.turgot.servlet.client;

import java.io.IOException;
import java.util.Properties;

import fr.turgot.dao.BilletDAO;
import fr.turgot.dao.FilmDAO;
import fr.turgot.dao.SeanceDAO;
import fr.turgot.dao.model.Film;
import fr.turgot.dao.model.Salle;
import fr.turgot.dao.model.Seance;
import fr.turgot.dao.model.Utilisateur;
import fr.turgot.utils.CsrfProtection;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(name = "/BilletServlet", value = "/billetterie")
public class BilletServlet extends HttpServlet {
    // La session pour récupérer les informations sur le billet
    private final BilletDAO billetDAO = new BilletDAO();
    // La session pour récupérer les informations sur la séance et le film
    private final SeanceDAO seanceDAO = new SeanceDAO();
    // La session pour récupérer les informations sur le film
    private final FilmDAO filmDAO = new FilmDAO();

    // GET : afficher le formulaire de réservation
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Utilisateur user = (Utilisateur) request.getSession().getAttribute("usere");
        if (user == null || !"CLIENT".equalsIgnoreCase(user.getTypeUtilisateur())) {
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return;
        }
        CsrfProtection.ensureToken(request);

        int seanceId = Integer.parseInt(request.getParameter("seanceId"));
        int filmId = Integer.parseInt(request.getParameter("filmId"));

        Seance seance = seanceDAO.getSeanceById(seanceId);
        Film film = filmDAO.getFilmById(filmId);
        Salle salle = billetDAO.getSalleBySeanceId(seanceId);
        int placesReservees = billetDAO.getPlacesReservees(seanceId);
        int placesDisponibles = salle.getCapacite() - placesReservees;

        request.setAttribute("seance", seance);
        request.setAttribute("film", film);
        request.setAttribute("salle", salle);
        request.setAttribute("placesDisponibles", placesDisponibles);

        getServletContext().getRequestDispatcher("/WEB-INF/client/billet.jsp")
                .forward(request, response);
    }

    // POST : enregistrer la réservation et envoyer le mail
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Utilisateur user = (Utilisateur) request.getSession().getAttribute("usere");
        if (user == null || !"CLIENT".equalsIgnoreCase(user.getTypeUtilisateur())) {
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return;
        }
        if (!CsrfProtection.isValid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Jeton CSRF invalide.");
            return;
        }
        CsrfProtection.ensureToken(request);

        int seanceId = Integer.parseInt(request.getParameter("seanceId"));
        int filmId = Integer.parseInt(request.getParameter("filmId"));
        int nbPlaces = Integer.parseInt(request.getParameter("nbPlaces"));

        Salle salle = billetDAO.getSalleBySeanceId(seanceId);
        int placesReservees = billetDAO.getPlacesReservees(seanceId);
        int placesDisponibles = salle.getCapacite() - placesReservees;

        if (nbPlaces < 1 || nbPlaces > placesDisponibles) {
            request.setAttribute("erreur", "Nombre de places invalide. Il reste " + placesDisponibles + " place(s).");
            Seance seance = seanceDAO.getSeanceById(seanceId);
            Film film = filmDAO.getFilmById(filmId);
            request.setAttribute("seance", seance);
            request.setAttribute("film", film);
            request.setAttribute("salle", salle);
            request.setAttribute("placesDisponibles", placesDisponibles);
            getServletContext().getRequestDispatcher("/WEB-INF/client/billet.jsp")
                    .forward(request, response);
            return;
        }

        if (!billetDAO.createReservation(seanceId, user.getId(), nbPlaces)) {
            request.setAttribute("erreur", "Le nombre de places demandé n'est plus disponible.");
            Seance seance = seanceDAO.getSeanceById(seanceId);
            Film film = filmDAO.getFilmById(filmId);
            request.setAttribute("seance", seance);
            request.setAttribute("film", film);
            request.setAttribute("salle", salle);
            request.setAttribute("placesDisponibles",
                    salle.getCapacite() - billetDAO.getPlacesReservees(seanceId));
            getServletContext().getRequestDispatcher("/WEB-INF/client/billet.jsp")
                    .forward(request, response);
            return;
        }

        Seance seance = seanceDAO.getSeanceById(seanceId);
        Film film = filmDAO.getFilmById(filmId);
        envoyerMail(user.getEmail(), user.getLogin(), film.getTitre(),
                seance.getDate(), seance.getHoraire(), nbPlaces, salle.getNom());

        request.setAttribute("succes", "Réservation confirmée ! Un email vous a été envoyé.");
        request.setAttribute("film", film);
        request.setAttribute("seance", seance);
        getServletContext().getRequestDispatcher("/WEB-INF/client/billet.jsp")
                .forward(request, response);
    }

    private void envoyerMail(String destinataire, String pseudo, String titreFilm,
    String date, String horaire, int nbPlaces, String nomSalle) {
        String host = "smtp-mail.outlook.com";
        String port = "587";
        String mailFrom = "Cinema.com@outlook.fr";
        String password = "Cinema.com12";

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);

        Session session = Session.getInstance(props, new jakarta.mail.Authenticator() {
            protected jakarta.mail.PasswordAuthentication getPasswordAuthentication() {
                return new jakarta.mail.PasswordAuthentication(mailFrom, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(mailFrom));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinataire));
            message.setSubject("Confirmation de réservation - " + titreFilm);
            message.setText(
                "Bonjour " + pseudo + ",\n\n" +
                "Votre réservation est confirmée !\n\n" +
                "Film : " + titreFilm + "\n" +
                "Date : " + date + "\n" +
                "Heure : " + horaire + "\n" +
                "Salle : " + nomSalle + "\n" +
                "Nombre de places : " + nbPlaces + "\n\n" +
                "À bientôt au cinéma !\n" +
                "L'équipe Cynemma"
            );
            Transport.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}