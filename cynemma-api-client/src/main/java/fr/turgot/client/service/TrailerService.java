package fr.turgot.client.service;

import java.util.Optional;

import fr.turgot.client.dto.TrailerResponse;

/**
 * Service de recherche et consultation des bandes-annonces de films.
 */
public interface TrailerService {
    /**
     * Recherche la bande-annonce d'un film par son titre.
     *
     * @param film titre du film (obligatoire)
     * @return la réponse contenant la clé et les URLs YouTube, ou {@link Optional#empty()} si absente.
     */
    Optional<TrailerResponse> getTrailer(String film);

    /**
     * Recherche la bande-annonce d'un film par son titre et son année de sortie.
     *
     * @param film titre du film (obligatoire)
     * @param year année de sortie (entre 1888 et 9999, facultatif)
     * @return la réponse contenant la clé et les URLs YouTube, ou {@link Optional#empty()} si absente.
     */
    Optional<TrailerResponse> getTrailer(String film, Integer year);
}
