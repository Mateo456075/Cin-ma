package fr.turgot.client.service;

import java.net.http.HttpResponse;
import org.json.JSONObject;

import fr.turgot.client.dto.HealthResponse;
import fr.turgot.client.http.HttpRequestExecutor;

public class DefaultHealthService implements HealthService {
    private final HttpRequestExecutor executor;

    public DefaultHealthService(HttpRequestExecutor executor) {
        this.executor = executor;
    }

    @Override
    public HealthResponse checkHealth() {
        HttpResponse<String> response = executor.executeStringRequest(
                "/api/health", "GET", null, null, "application/json");
        JSONObject json = new JSONObject(response.body());
        return new HealthResponse(
                json.optString("status", "UNKNOWN"),
                json.optString("database", "UNKNOWN"));
    }
}
