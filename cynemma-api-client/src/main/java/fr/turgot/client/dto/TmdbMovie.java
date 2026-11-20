package fr.turgot.client.dto;

import java.util.Objects;

/**
 * Représentation canonique d'un film provenant de l'API Cynemma (TMDB).
 */
public record TmdbMovie(
        int tmdbId,
        String title,
        String overview,
        String releaseDate,
        String posterPath,
        Integer runtimeMinutes,
        String director,
        String genres) {

    public TmdbMovie {
        title = title == null ? "" : title;
        overview = overview == null ? "" : overview;
        releaseDate = releaseDate == null ? "" : releaseDate;
        director = director == null ? "" : director;
        genres = genres == null ? "" : genres;
    }

    /**
     * Retourne l'année de sortie sur 4 chiffres extraite de la date de sortie.
     */
    public String getReleaseYear() {
        if (releaseDate == null || releaseDate.length() < 4) {
            return "";
        }
        return releaseDate.substring(0, 4);
    }

    /**
     * Retourne l'URL complète CDN vers l'affiche officielle TMDB en résolution w500.
     */
    public String getPosterUrl() {
        if (posterPath == null || posterPath.isBlank()) {
            return null;
        }
        return "https://image.tmdb.org/t/p/w500" + (posterPath.startsWith("/") ? posterPath : "/" + posterPath);
    }

    // Accesseurs JavaBeans pour compatibilité totale avec JSTL et frameworks existants
    public int getTmdbId() {
        return tmdbId;
    }

    public String getTitle() {
        return title;
    }

    public String getOverview() {
        return overview;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public String getPosterPath() {
        return posterPath;
    }

    public Integer getRuntimeMinutes() {
        return runtimeMinutes;
    }

    public String getDirector() {
        return director;
    }

    public String getGenres() {
        return genres;
    }
}
