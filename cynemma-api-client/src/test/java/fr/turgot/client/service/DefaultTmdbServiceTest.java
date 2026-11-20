package fr.turgot.client.service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import com.sun.net.httpserver.HttpServer;
import fr.turgot.client.auth.StaticTokenProvider;
import fr.turgot.client.dto.TmdbMovie;
import fr.turgot.client.http.HttpRequestExecutor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DefaultTmdbServiceTest {

    private static HttpServer server;
    private static TmdbService tmdbService;

    @BeforeAll
    static void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = server.getAddress().getPort();

        server.createContext("/cynemma-api/api/internal/tmdb/now-playing", exchange -> {
            String body = """
                    {
                        "page": 1,
                        "total_pages": 1,
                        "results": [
                            {
                                "id": 101,
                                "title": "Dune",
                                "overview": "Desert planet",
                                "release_date": "2021-09-15",
                                "poster_path": "/dune.jpg",
                                "backdrop_path": "/dune_back.jpg",
                                "vote_average": 8.1,
                                "runtime": 155
                            }
                        ]
                    }
                    """;
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });

        server.createContext("/cynemma-api/api/internal/tmdb/movies/202", exchange -> {
            String path = exchange.getRequestURI().getPath();
            if (path.endsWith("/poster")) {
                byte[] img = new byte[]{0x01, 0x02, 0x03};
                exchange.getResponseHeaders().set("Content-Type", "image/jpeg");
                exchange.sendResponseHeaders(200, img.length);
                exchange.getResponseBody().write(img);
            } else if (path.endsWith("/trailer-cache")) {
                exchange.sendResponseHeaders(204, -1);
            } else {
                String body = """
                        {
                            "id": 202,
                            "title": "Interstellar",
                            "overview": "Space exploration",
                            "release_date": "2014-11-05",
                            "vote_average": 8.6
                        }
                        """;
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            }
            exchange.close();
        });

        server.start();

        HttpClient client = HttpClient.newHttpClient();
        HttpRequestExecutor executor = new HttpRequestExecutor(
                "http://127.0.0.1:" + port + "/cynemma-api",
                client,
                Duration.ofSeconds(5),
                new StaticTokenProvider("mock-token")
        );
        tmdbService = new DefaultTmdbService(executor);
    }

    @AfterAll
    static void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("getNowPlaying returns movie list")
    void getNowPlayingReturnsMovieList() {
        List<TmdbMovie> movies = tmdbService.getNowPlaying();
        assertNotNull(movies);
        assertEquals(1, movies.size());
        TmdbMovie movie = movies.get(0);
        assertEquals(101, movie.tmdbId());
        assertEquals("Dune", movie.title());
        assertEquals("2021", movie.getReleaseYear());
        assertEquals("https://image.tmdb.org/t/p/w500/dune.jpg", movie.getPosterUrl());
    }

    @Test
    @DisplayName("getMovieDetails returns parsed movie")
    void getMovieDetailsReturnsMovie() {
        TmdbMovie movie = tmdbService.getMovieDetails(202);
        assertNotNull(movie);
        assertEquals(202, movie.tmdbId());
        assertEquals("Interstellar", movie.title());
        assertEquals("2014", movie.getReleaseYear());
    }

    @Test
    @DisplayName("downloadPoster returns byte array")
    void downloadPosterReturnsByteArray() {
        byte[] result = tmdbService.downloadPoster(202);
        assertArrayEquals(new byte[]{0x01, 0x02, 0x03}, result);
    }

    @Test
    @DisplayName("cacheTrailer sends post request")
    void cacheTrailerSendsPost() {
        assertDoesNotThrow(() -> tmdbService.cacheTrailer(202, "Interstellar"));
    }
}
