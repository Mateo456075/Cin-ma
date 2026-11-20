package fr.turgot.client;

import java.net.http.HttpClient;
import java.time.Duration;

import fr.turgot.client.auth.StaticTokenProvider;
import fr.turgot.client.auth.TokenProvider;
import fr.turgot.client.http.HttpRequestExecutor;

/**
 * Constructeur fluide (Builder) pour CynemmaApiClient.
 */
public class CynemmaApiClientBuilder {
    private String baseUrl;
    private HttpClient httpClient;
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration requestTimeout = Duration.ofSeconds(15);
    private TokenProvider tokenProvider;

    public CynemmaApiClientBuilder baseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public CynemmaApiClientBuilder httpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
        return this;
    }

    public CynemmaApiClientBuilder connectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    public CynemmaApiClientBuilder requestTimeout(Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
        return this;
    }

    public CynemmaApiClientBuilder tokenProvider(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
        return this;
    }

    public CynemmaApiClientBuilder bearerToken(String staticToken) {
        this.tokenProvider = staticToken != null ? new StaticTokenProvider(staticToken) : null;
        return this;
    }

    public CynemmaApiClient build() {
        HttpClient client = this.httpClient;
        if (client == null) {
            client = HttpClient.newBuilder()
                    .connectTimeout(connectTimeout != null ? connectTimeout : Duration.ofSeconds(5))
                    .build();
        }

        HttpRequestExecutor executor = new HttpRequestExecutor(
                baseUrl,
                client,
                requestTimeout,
                tokenProvider);

        return new DefaultCynemmaApiClient(executor);
    }
}
