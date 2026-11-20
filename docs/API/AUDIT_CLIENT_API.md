# Rapport d'Audit Préalable : Architecture API et Client SDK Cynemma

Ce document formalise l'audit exhaustif des composants d'échanges réseau entre l'application principale **`cynemma`** (WAR) et le microservice **`cynemma-api`** (WAR), préalable à la création de la bibliothèque cliente autonome **`cynemma-api-client`** (JAR).

---

## 1. Liste Complète des Endpoints Réellement Disponibles

L'audit du code source de `API/` ([HealthServlet.java](file:///Volumes/MateoDISQ/Cin-ma/API/src/main/java/fr/turgot/api/controller/HealthServlet.java), [TrailerServlet.java](file:///Volumes/MateoDISQ/Cin-ma/API/src/main/java/fr/turgot/api/controller/TrailerServlet.java), [InternalTmdbServlet.java](file:///Volumes/MateoDISQ/Cin-ma/API/src/main/java/fr/turgot/api/controller/InternalTmdbServlet.java)) confirme l'existence de **6 routes REST effectives**. Aucun autre endpoint n'est présent dans le code de l'API.

| Endpoint | Méthode HTTP | Type d'accès | Rôle requis | Format réponse |
|---|---|---|---|---|
| `/api/health` | `GET` | Public | Aucun | `application/json` |
| `/api/trailer` | `GET` | Public | Aucun | `application/json` |
| `/api/internal/tmdb/now-playing` | `GET` | M2M Authentifié | `EMPLOYE` (via JWT) | `application/json` |
| `/api/internal/tmdb/movies/{tmdbId}` | `GET` | M2M Authentifié | `EMPLOYE` (via JWT) | `application/json` |
| `/api/internal/tmdb/movies/{tmdbId}/poster` | `GET` | M2M Authentifié | `EMPLOYE` (via JWT) | `image/jpeg` ou `image/png` |
| `/api/internal/tmdb/movies/{tmdbId}/trailer-cache` | `POST` | M2M Authentifié | `EMPLOYE` (via JWT) | `application/json` (ou 204) |

---

## 2. Méthodes HTTP, Paramètres et Formats JSON

### 2.1. `GET /api/health`
- **Description** : Vérification de la disponibilité du microservice et de la connectivité avec la base MySQL via DataSource JNDI.
- **Paramètres** : Aucun.
- **Réponse HTTP 200 (UP)** :
  ```json
  { "status": "UP", "database": "UP" }
  ```
- **Réponse HTTP 503 (DOWN)** :
  ```json
  { "status": "DOWN", "database": "DOWN" }
  ```

### 2.2. `GET /api/trailer`
- **Description** : Recherche de bande-annonce YouTube via cache local MySQL ou interrogation externe TMDB.
- **Paramètres de requête (Query params)** :
  - `film` (String, obligatoire, non blanc) : titre du film.
  - `year` (Integer, facultatif, compris entre 1888 et 9999) : année de sortie.
- **Réponse HTTP 200 (Succès)** :
  ```json
  {
    "key": "zSWdZVtXT7E",
    "watchUrl": "https://www.youtube.com/watch?v=zSWdZVtXT7E",
    "embedUrl": "https://www.youtube-nocookie.com/embed/zSWdZVtXT7E"
  }
  ```
- **Codes d'erreur** :
  - `400 Bad Request` : paramètre `film` manquant ou `year` invalide (`{"erreur": "..."}`).
  - `404 Not Found` : aucune bande-annonce trouvée (`{"erreur": "Aucune bande-annonce disponible"}`).
  - `502 Bad Gateway` : échec de communication avec TMDB.
  - `503 Service Unavailable` : clé TMDB non configurée ou recherche interrompue.
  - `504 Gateway Timeout` : délai d'attente TMDB dépassé.
  - `500 Internal Server Error` : erreur SQL de cache.

### 2.3. `GET /api/internal/tmdb/now-playing`
- **Description** : Liste des suggestions de films à l'affiche fournies par TMDB.
- **En-tête requis** : `Authorization: Bearer <jwt_rs256>` (rôle `EMPLOYE`).
- **Paramètres** : Aucun.
- **Réponse HTTP 200 (Succès)** :
  ```json
  {
    "results": [
      {
        "tmdbId": 157336,
        "title": "Interstellar",
        "overview": "Une équipe d'explorateurs voyage à travers un trou de ver...",
        "releaseDate": "2014-11-05",
        "posterPath": "/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg",
        "posterUrl": "https://image.tmdb.org/t/p/w500/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg",
        "runtimeMinutes": 169,
        "director": "Christopher Nolan",
        "genres": "Aventure, Drame, Science-Fiction"
      }
    ]
  }
  ```
- **Codes d'erreur** : `401 Unauthorized` (jeton absent/invalide/expiré), `403 Forbidden` (rôle non-employé), `502`, `503`, `504`, `500`.

### 2.4. `GET /api/internal/tmdb/movies/{tmdbId}`
- **Description** : Récupération des métadonnées complètes d'un film TMDB spécifique.
- **En-tête requis** : `Authorization: Bearer <jwt_rs256>` (rôle `EMPLOYE`).
- **Paramètre de chemin (Path param)** : `tmdbId` (entier strictement positif).
- **Réponse HTTP 200 (Succès)** : Objet JSON représentant le film (mêmes champs que dans `results`).
- **Codes d'erreur** : `400 Bad Request` (id non numérique ou <= 0), `401`, `403`, `404`, `502`, `503`, `500`.

### 2.5. `GET /api/internal/tmdb/movies/{tmdbId}/poster`
- **Description** : Téléchargement direct du fichier binaire de l'affiche du film en haute définition.
- **En-tête requis** : `Authorization: Bearer <jwt_rs256>`.
- **Paramètre de chemin** : `tmdbId` (entier positif).
- **Réponse HTTP 200 (Succès)** :
  - Content-Type : `image/jpeg` ou `image/png`.
  - Body : flux binaire brut de l'image (`byte[]`).
- **Codes d'erreur** : `401`, `403`, `404` (pas d'affiche associée), `502`, `503`, `500`.

### 2.6. `POST /api/internal/tmdb/movies/{tmdbId}/trailer-cache`
- **Description** : Déclenche la mise en cache anticipée de la bande-annonce pour le film importé.
- **En-têtes requis** : `Authorization: Bearer <jwt_rs256>`, `Content-Type: application/json; charset=UTF-8`.
- **Paramètre de chemin** : `tmdbId` (entier positif).
- **Corps de requête (Body JSON)** :
  ```json
  { "title": "Interstellar" }
  ```
  *(Taille maximale autorisée : 4096 octets).*
- **Réponse HTTP 200 ou 204** : `{"available": true}` ou pas de contenu.
- **Codes d'erreur** : `400` (titre absent/blanc ou JSON malformé), `413` (corps > 4096 octets), `415` (Content-Type non JSON UTF-8), `401`, `403`, `500`.

---

## 3. DTOs Associés

| DTO | Rôle | Champs principaux | Remarques |
|---|---|---|---|
| `TmdbMovie` | Métadonnées d'un film TMDB | `tmdbId` (int), `title` (String), `overview` (String), `releaseDate` (String), `posterPath` (String, nullable), `posterUrl` (String, nullable), `runtimeMinutes` (Integer, nullable), `director` (String), `genres` (String) | Actuellement dupliqué sous forme de `record` dans `API` et sous forme de `class` dans `cynemma`. Le client JAR fournira une version canonique unifiée avec accesseurs `getReleaseYear()` et `getPosterUrl()`. |
| `TrailerResponse` | Résultat de recherche de bande-annonce | `key` (String), `watchUrl` (String), `embedUrl` (String) | Format standard retourné par `/api/trailer`. |
| `HealthResponse` | État de santé du service | `status` (String: UP/DOWN), `database` (String: UP/DOWN) | Format retourné par `/api/health`. |
| `TrailerCacheRequest` | Demande de pré-mise en cache | `title` (String, obligatoire) | Utilisé lors de l'import TMDB. |

---

## 4. Mécanismes de Sécurité & Gestion JWT

- **Algorithme** : `RS256` (signature asymétrique RSA SHA-256 sur clé privée 2048 bits PKCS#8).
- **Vérification** : Réalisée dans [JwtTokenVerifier.java](file:///Volumes/MateoDISQ/Cin-ma/API/src/main/java/fr/turgot/api/security/JwtTokenVerifier.java) via clé publique X.509 (`CYNEMMA_JWT_PUBLIC_KEY`).
- **Claims obligatoires** :
  - `iss`: `"cynemma-main"`
  - `aud`: `"cynemma-api"`
  - `sub`: identifiant de l'employé connecté (non blanc)
  - `role`: `"EMPLOYE"`
  - `iat`: horodatage de génération
  - `exp`: expiration stricte (durée maximale de validité : **300 secondes / 5 minutes**).
- **Principe pour le client JAR** :
  - Le SDK ne doit **jamais** contenir de clé privée en dur.
  - Le client fournira une interface fonctionnelle d'injection de jeton :
    ```java
    @FunctionalInterface
    public interface TokenProvider {
        String getToken();
    }
    ```
  - L'application `cynemma` injectera simplement son service existant `ApiJwtTokenService::issueEmployeeToken` lors de l'instanciation du client ou passera le login/token requis.
  - Zéro fuite de jeton dans les logs : masquage systématique des en-têtes Authorization (`Bearer ***`).

---

## 5. Classes Actuelles de `cynemma` Communiquant avec l'API

L'audit révèle que **toutes les communications réseau** de l'application `cynemma` vers `cynemma-api` sont actuellement concentrées dans :

1. [cynemma/src/main/java/fr/turgot/service/TmdbApiClient.java](file:///Volumes/MateoDISQ/Cin-ma/cynemma/src/main/java/fr/turgot/service/TmdbApiClient.java) :
   - Classe interne effectuant les requêtes `HttpClient`, le parsing JSON avec `org.json`, la validation d'URL et la gestion des codes HTTP.
2. [cynemma/src/main/java/fr/turgot/api/controller/TrailerServlet.java](file:///Volumes/MateoDISQ/Cin-ma/cynemma/src/main/java/fr/turgot/api/controller/TrailerServlet.java) :
   - Servlet proxy relais exposant `/api/trailer` côté application principale et déléguant l'appel à `TmdbApiClient.getTrailer()`.
3. [cynemma/src/main/java/fr/turgot/servlet/employe/TmdbCatalogImportServlet.java](file:///Volumes/MateoDISQ/Cin-ma/cynemma/src/main/java/fr/turgot/servlet/employe/TmdbCatalogImportServlet.java) :
   - Servlet employée appelant `TmdbApiClient.getNowPlayingMovies()`, `getMovieDetails()`, `downloadPoster()` et `cacheTrailerForMovie()`.
4. [cynemma/src/main/java/fr/turgot/dto/TmdbMovie.java](file:///Volumes/MateoDISQ/Cin-ma/cynemma/src/main/java/fr/turgot/dto/TmdbMovie.java) :
   - DTO interne dupliqué.

---

## 6. Périmètre Exact des Fonctionnalités à Transférer dans le Client JAR

Le client JAR `cynemma-api-client` prendra en charge les **6 opérations réelles** :
1. `checkHealth()` : vérification santé (`GET /api/health`).
2. `getTrailer(String film, Integer year)` : obtention de la bande-annonce (`GET /api/trailer`).
3. `getNowPlaying(String employeeLogin)` (ou avec `TokenProvider`) : catalogue TMDB (`GET /api/internal/tmdb/now-playing`).
4. `getMovieDetails(int tmdbId, String employeeLogin)` : détails film TMDB (`GET /api/internal/tmdb/movies/{tmdbId}`).
5. `downloadPoster(int tmdbId, String employeeLogin)` : téléchargement image (`GET /api/internal/tmdb/movies/{tmdbId}/poster`).
6. `cacheTrailer(int tmdbId, String title, String employeeLogin)` : pré-cache bande-annonce (`POST /api/internal/tmdb/movies/{tmdbId}/trailer-cache`).

> **Note de conformité** : Conformément à la consigne impérative de l'utilisateur (*« Ne crée pas de méthodes clientes pour des endpoints qui n'existent pas réellement »*), aucune méthode fictive (réservation, sièges 3D, paiements) ne sera injectée dans le SDK tant que les endpoints correspondants ne sont pas développés dans `cynemma-api`.

---

## 7. Difficultés & Points d'Attention Identifiés

1. **Validation stricte de l'URL de base (`buildApiUri`)** :
   - L'API n'accepte HTTP qu'en boucle locale (`localhost`, `127.0.0.1`, `::1`). En environnement distant, HTTPS est obligatoire. Cette règle de sécurité éprouvée sera conservée dans le client.
2. **Durée de vie courte des jetons JWT (5 minutes)** :
   - Les jetons JWT expirent en 300 secondes. Le client doit supporter la fourniture dynamique d'un nouveau jeton à chaque requête (via `TokenProvider` ou `Supplier<String>`).
3. **Contrôle strict des flux binaires d'images** :
   - Le téléchargement d'affiche doit vérifier que le Content-Type retourné est bien `image/jpeg` ou `image/png` afin de refuser toute réponse inattendue.
4. **Hiérarchie d'exceptions typée** :
   - Éviter de propager des `IOException` génériques. Fournir `CynemmaApiException` (classe mère) avec spécialisations : `CynemmaAuthenticationException` (401), `CynemmaForbiddenException` (403), `CynemmaNotFoundException` (404), `CynemmaValidationException` (400), `CynemmaServiceUnavailableException` (502/503/504).
