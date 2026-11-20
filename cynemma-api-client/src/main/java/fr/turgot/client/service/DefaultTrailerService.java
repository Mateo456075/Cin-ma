package fr.turgot.client.service;

import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.json.JSONObject;

import fr.turgot.client.dto.TrailerResponse;
import fr.turgot.client.exception.CynemmaNotFoundException;
import fr.turgot.client.http.HttpRequestExecutor;

public class DefaultTrailerService implements TrailerService {
    private final HttpRequestExecutor executor;

    public DefaultTrailerService(HttpRequestExecutor executor) {
        this.executor = executor;
    }

    @Override
    public Optional<TrailerResponse> getTrailer(String film) {
        return getTrailer(film, null);
    }

    @Override
    public Optional<TrailerResponse> getTrailer(String film, Integer year) {
        if (film == null || film.isBlank()) {
            throw new IllegalArgumentException("Le titre du film est obligatoire.");
        }

        StringBuilder path = new StringBuilder("/api/trailer?film=")
                .append(URLEncoder.encode(film.trim(), StandardCharsets.UTF_8));
        if (year != null) {
            if (year < 1888 || year > 9999) {
                throw new IllegalArgumentException("L'année doit être comprise entre 1888 et 9999.");
            }
            path.append("&year=").append(year);
        }

        try {
            HttpResponse<String> response = executor.executeStringRequest(
                    path.toString(), "GET", null, null, "application/json");
            JSONObject json = new JSONObject(response.body());
            return Optional.of(new TrailerResponse(
                    json.getString("key"),
                    json.getString("watchUrl"),
                    json.getString("embedUrl")));
        } catch (CynemmaNotFoundException e) {
            return Optional.empty();
        }
    }
}
