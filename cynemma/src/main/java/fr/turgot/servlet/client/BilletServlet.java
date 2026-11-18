package fr.turgot.servlet.client;

import java.io.IOException;
import java.time.LocalDateTime;
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
import jakarta.servlet.http.HttpSession;

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
        Utilisateur user = getCurrentClient(request);
        if (user == null || !"CLIENT".equalsIgnoreCase(user.getTypeUtilisateur())) {
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return;
        }
        CsrfProtection.ensureToken(request);

        BookingContext booking;
        try {
            booking = loadBooking(
                    parsePositiveParameter(request, "seanceId"),
                    parsePositiveParameter(request, "filmId"));
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Les identifiants de séance et de film doivent être des entiers positifs.");
            return;
        }
        if (booking == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "La séance, le film ou la salle demandée est introuvable.");
            return;
        }
        if (!isBookable(booking.seance())) {
            response.sendError(HttpServletResponse.SC_CONFLICT,
                    "Cette séance n'est plus réservable.");
            return;
        }

        renderBookingForm(request, response, booking,
                billetDAO.getPlacesDisponibles(booking.seance().getId()));
    }

    // POST : enregistrer la réservation et envoyer le mail
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Utilisateur user = getCurrentClient(request);
        if (user == null || !"CLIENT".equalsIgnoreCase(user.getTypeUtilisateur())) {
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return;
        }
        if (!CsrfProtection.isValid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Jeton CSRF invalide.");
            return;
        }
        CsrfProtection.ensureToken(request);

        int seanceId;
        int filmId;
        int nbPlaces;
        try {
            seanceId = parsePositiveParameter(request, "seanceId");
            filmId = parsePositiveParameter(request, "filmId");
            nbPlaces = parsePositiveParameter(request, "nbPlaces");
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Les identifiants et le nombre de places doivent être des entiers positifs.");
            return;
        }

        BookingContext booking = loadBooking(seanceId, filmId);
        if (booking == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "La séance, le film ou la salle demandée est introuvable.");
            return;
        }
        if (!isBookable(booking.seance())) {
            response.sendError(HttpServletResponse.SC_CONFLICT,
                    "Cette séance n'est plus réservable.");
            return;
        }

        int placesDisponibles = billetDAO.getPlacesDisponibles(seanceId);
        if (nbPlaces > placesDisponibles) {
            request.setAttribute("erreur",
                    "Le nombre de places demandé dépasse les places réellement disponibles.");
            renderBookingForm(request, response, booking, placesDisponibles);
            return;
        }

        if (!billetDAO.createReservation(seanceId, user.getId(), nbPlaces)) {
            request.setAttribute("erreur", "Le nombre de places demandé n'est plus disponible.");
            renderBookingForm(request, response, booking,
                    billetDAO.getPlacesDisponibles(seanceId));
            return;
        }

        Seance seance = booking.seance();
        Film film = booking.film();
        Salle salle = booking.salle();
        boolean emailEnvoye = envoyerMail(user.getEmail(), user.getLogin(), film.getTitre(),
                seance.getDate(), seance.getHoraire(), nbPlaces, salle.getNom());

        request.setAttribute("succes", emailEnvoye
                ? "Réservation confirmée ! Un email vous a été envoyé."
                : "Réservation confirmée, mais l'email de confirmation n'a pas pu être envoyé.");
        request.setAttribute("film", film);
        request.setAttribute("seance", seance);
        request.setAttribute("salle", salle);
        getServletContext().getRequestDispatcher("/WEB-INF/client/billet.jsp")
                .forward(request, response);
    }

    static int parsePositiveInteger(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("La valeur est obligatoire.");
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed <= 0) {
                throw new IllegalArgumentException("La valeur doit être positive.");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("La valeur doit être un entier.", e);
        }
    }

    private static int parsePositiveParameter(HttpServletRequest request, String name) {
        return parsePositiveInteger(request.getParameter(name));
    }

    private Utilisateur getCurrentClient(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (Utilisateur) session.getAttribute("usere");
    }

    private BookingContext loadBooking(int seanceId, int filmId) {
        Seance seance = seanceDAO.getSeanceById(seanceId);
        Film film = filmDAO.getFilmById(filmId);
        if (seance == null || film == null || seance.getFilmId() != filmId) {
            return null;
        }
        Salle salle = billetDAO.getSalleBySeanceId(seanceId);
        return salle == null ? null : new BookingContext(seance, film, salle);
    }

    private boolean isBookable(Seance seance) {
        LocalDateTime start = seance.getDateHeure();
        return start != null && start.isAfter(LocalDateTime.now());
    }

    private void renderBookingForm(HttpServletRequest request, HttpServletResponse response,
            BookingContext booking, int placesDisponibles) throws ServletException, IOException {
        request.setAttribute("seance", booking.seance());
        request.setAttribute("film", booking.film());
        request.setAttribute("salle", booking.salle());
        request.setAttribute("placesDisponibles", placesDisponibles);
        getServletContext().getRequestDispatcher("/WEB-INF/client/billet.jsp")
                .forward(request, response);
    }

    private record BookingContext(Seance seance, Film film, Salle salle) {
    }

    private boolean envoyerMail(String destinataire, String pseudo, String titreFilm,
            String date, String horaire, int nbPlaces, String nomSalle) {
        String host = "smtp-mail.outlook.com";
        String port = "587";
        String mailFrom = System.getenv("SMTP_USERNAME");
        String password = System.getenv("SMTP_PASSWORD");
        if (mailFrom == null || mailFrom.isBlank() || password == null || password.isBlank()) {
            getServletContext().log(
                    "SMTP_USERNAME et SMTP_PASSWORD doivent être configurées pour envoyer les confirmations.");
            return false;
        }

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
            return true;
        } catch (MessagingException e) {
            getServletContext().log("Impossible d'envoyer l'email de confirmation de réservation.", e);
            return false;
        }
    }
}