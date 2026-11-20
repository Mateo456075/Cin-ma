package fr.turgot.client.service;

import java.util.List;

import fr.turgot.client.dto.TmdbMovie;

/**
 * Service d'interaction avec le catalogue TMDB interne réservé aux employés.
 */
public interface TmdbService {

    /**
     * Récupère la liste des films actuellement à l'affiche proposés par TMDB.
     * Utilise le TokenProvider par défaut configuré sur le client.
     *
     * @return la liste des suggestions de films.
     */
    List<TmdbMovie> getNowPlaying();

    /**
     * Récupère la liste des films actuellement à l'affiche avec un jeton employé explicite.
     *
     * @param employeeToken jeton JWT Bearer de l'employé
     * @return la liste des suggestions de films.
     */
    List<TmdbMovie> getNowPlaying(String employeeToken);

    /**
     * Récupère les métadonnées complètes d'un film TMDB par son identifiant.
     *
     * @param tmdbId identifiant TMDB positif
     * @return le film TMDB correspondant.
     */
    TmdbMovie getMovieDetails(int tmdbId);

    /**
     * Récupère les métadonnées complètes d'un film TMDB par son identifiant avec un jeton explicite.
     *
     * @param tmdbId        identifiant TMDB positif
     * @param employeeToken jeton JWT Bearer de l'employé
     * @return le film TMDB correspondant.
     */
    TmdbMovie getMovieDetails(int tmdbId, String employeeToken);

    /**
     * Télécharge l'affiche haute définition d'un film TMDB en JPEG ou PNG.
     *
     * @param tmdbId identifiant TMDB positif
     * @return le contenu binaire de l'image.
     */
    byte[] downloadPoster(int tmdbId);

    /**
     * Télécharge l'affiche haute définition d'un film TMDB avec un jeton explicite.
     *
     * @param tmdbId        identifiant TMDB positif
     * @param employeeToken jeton JWT Bearer de l'employé
     * @return le contenu binaire de l'image.
     */
    byte[] downloadPoster(int tmdbId, String employeeToken);

    /**
     * Déclenche la mise en cache anticipée de la bande-annonce pour un film TMDB.
     *
     * @param tmdbId identifiant TMDB positif
     * @param title  titre du film
     */
    void cacheTrailer(int tmdbId, String title);

    /**
     * Déclenche la mise en cache anticipée de la bande-annonce avec un jeton explicite.
     *
     * @param tmdbId        identifiant TMDB positif
     * @param title         titre du film
     * @param employeeToken jeton JWT Bearer de l'employé
     */
    void cacheTrailer(int tmdbId, String title, String employeeToken);
}
