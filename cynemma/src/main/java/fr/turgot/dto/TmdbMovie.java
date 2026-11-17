package fr.turgot.dto;

public class TmdbMovie {

    private final int tmdbId;
    private final String title;
    private final String overview;
    private final String releaseDate;
    private final String posterPath;
    private final Integer runtimeMinutes;
    private final String director;
    private final String genres;

    public TmdbMovie(
            int tmdbId,
            String title,
            String overview,
            String releaseDate,
            String posterPath,
            Integer runtimeMinutes,
            String director,
            String genres) {
        this.tmdbId = tmdbId;
        this.title = title;
        this.overview = overview;
        this.releaseDate = releaseDate;
        this.posterPath = posterPath;
        this.runtimeMinutes = runtimeMinutes;
        this.director = director;
        this.genres = genres;
    }

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

    public String getReleaseYear() {
        if (releaseDate == null || releaseDate.length() < 4) {
            return "";
        }
        return releaseDate.substring(0, 4);
    }

    public String getPosterPath() {
        return posterPath;
    }

    public String getPosterUrl() {
        return posterPath == null ? null : "https://image.tmdb.org/t/p/w500" + posterPath;
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
