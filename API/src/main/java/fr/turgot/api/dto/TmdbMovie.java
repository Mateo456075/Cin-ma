package fr.turgot.api.dto;

public record TmdbMovie(
        int tmdbId,
        String title,
        String overview,
        String releaseDate,
        String posterPath,
        Integer runtimeMinutes,
        String director,
        String genres) {
}
