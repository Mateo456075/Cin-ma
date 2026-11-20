# Guide d'Utilisation du SDK `cynemma-api-client`

## 1. Création du client avec le Builder

L'entrée principale du SDK est l'interface `CynemmaApiClient`. Une instance se configure via `CynemmaApiClient.builder()` :

```java
import fr.turgot.client.CynemmaApiClient;
import fr.turgot.client.auth.StaticTokenProvider;
import java.time.Duration;

// Exemple avec token statique ou fournisseur dynamique
CynemmaApiClient client = CynemmaApiClient.builder()
        .baseUrl("http://localhost:8081/cynemma-api") // ou https://api.cynemma.fr
        .tokenProvider(() -> ApiJwtTokenService.issueEmployeeToken("alice"))
        .connectTimeout(Duration.ofSeconds(5))
        .requestTimeout(Duration.ofSeconds(15))
        .build();
```

---

## 2. Vérification de Santé (`HealthService`)

```java
import fr.turgot.client.dto.HealthResponse;

HealthResponse health = client.health().checkHealth();
System.out.println("Statut API : " + health.status());
System.out.println("Base de données : " + health.database());

if (health.isHealthy()) {
    System.out.println("L'API et la BDD sont pleinement opérationnelles.");
}
```

---

## 3. Recherche de Bandes-Annonces (`TrailerService`)

L'endpoint de bande-annonce est public et retourne un `Optional<TrailerResponse>` :

```java
import java.util.Optional;
import fr.turgot.client.dto.TrailerResponse;

// Recherche par titre et année (optionnelle)
Optional<TrailerResponse> trailer = client.trailers().getTrailer("Inception", 2010);

if (trailer.isPresent()) {
    TrailerResponse data = trailer.get();
    System.out.println("ID YouTube : " + data.key());
    System.out.println("Lien de visionnage : " + data.watchUrl());
    System.out.println("Lien d'intégration (iframe) : " + data.embedUrl());
} else {
    System.out.println("Aucune bande-annonce trouvée pour ce film.");
}
```

---

## 4. Catalogue TMDB et Affichage (`TmdbService`)

Ces méthodes interagissent avec les endpoints internes nécessitant un token JWT avec le rôle `EMPLOYE`.

### 4.1 Récupérer les suggestions à l'affiche
```java
import java.util.List;
import fr.turgot.client.dto.TmdbMovie;

List<TmdbMovie> movies = client.tmdb().getNowPlaying();
for (TmdbMovie movie : movies) {
    System.out.println(movie.tmdbId() + " : " + movie.title() + " (" + movie.getReleaseYear() + ")");
    System.out.println("URL affiche : " + movie.getPosterUrl());
}
```

### 4.2 Consulter le détail d'un film
```java
TmdbMovie movie = client.tmdb().getMovieDetails(550);
System.out.println("Titre : " + movie.title());
System.out.println("Synopsis : " + movie.overview());
System.out.println("Durée : " + movie.runtimeMinutes() + " min");
System.out.println("Réalisateur : " + movie.director());
System.out.println("Genres : " + movie.genres());
```

### 4.3 Télécharger l'affiche en haute définition
```java
byte[] imageBytes = client.tmdb().downloadPoster(550);
// Sauvegarde locale ou streaming vers l'utilisateur
Files.write(Path.of("/tmp/poster_550.jpg"), imageBytes);
```

### 4.4 Déclencher la mise en cache de la bande-annonce
```java
client.tmdb().cacheTrailer(550, "Fight Club");
```

---

## 5. Fourniture explicite de token par appel

Si vous ne configurez pas de `TokenProvider` global sur le client, vous pouvez passer le jeton directement à chaque appel de service :

```java
String employeeJwt = ApiJwtTokenService.issueEmployeeToken("bob");

List<TmdbMovie> movies = client.tmdb().getNowPlaying(employeeJwt);
TmdbMovie movie = client.tmdb().getMovieDetails(550, employeeJwt);
byte[] poster = client.tmdb().downloadPoster(550, employeeJwt);
client.tmdb().cacheTrailer(550, "Fight Club", employeeJwt);
```
