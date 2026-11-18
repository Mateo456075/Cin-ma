# Vue d'ensemble technique

## Architecture déployée

Cynemma est composé de deux WAR qui restent intégrés au même produit :

- `cynemma/` est l'application principale Jakarta Servlet/JSP, avec les pages,
  les sessions, les formulaires MVC, les DAO et la persistance métier JPA.
- `API/` est un projet Maven autonome qui produit le WAR des routes JSON. Il
  possède les appels TMDB et le DAO JDBC de `tmdb_cache`.

Les deux conteneurs utilisent la même base MySQL, mais chacun doit configurer sa
propre DataSource JNDI `java:comp/env/jdbc/cynema`. Le WAR API ne partage pas
l'EntityManager de l'application principale. Les opérations HTTP entre WAR ne
font pas partie d'une transaction SQL/JPA distribuée.

```text
Client web
  -> Cynemma principal (JSP, MVC, session employé, CSRF)
       -> /api/trailer (relais HTTP conservant le chemin historique)
            -> API indépendante
                 -> TmdbService
                      -> TmdbCacheDAO / tmdb_cache (JDBC)
                      -> TMDB (cache miss/expiration)

Import employé
  -> TmdbCatalogImportServlet (session, rôle EMPLOYE, CSRF, validation)
       -> TmdbApiClient (HTTP serveur-à-serveur + JWT RS256)
            -> API /api/internal/tmdb/*
       -> FilmDAO / JPA (enregistrement du film dans l'application principale)
```

Le parcours d'import conserve les pages de revue et de suggestions. Il ne
contourne pas le rôle, la validation ni le jeton CSRF de l'application
principale. Le JWT de cinq minutes permet à l'API d'autoriser ces appels internes
sans partager la session HTTP du navigateur.

## Endpoints JSON de l'API indépendante

| Méthode et route | Fonction |
|---|---|
| `GET /api/health` | Contrôle la connexion MySQL de l'API |
| `GET /api/trailer?film=...&year=...` | Recherche la bande-annonce avec cache |
| `GET /api/internal/tmdb/now-playing` | Suggestions employé depuis TMDB |
| `GET /api/internal/tmdb/movies/{tmdbId}` | Détails préremplis |
| `GET /api/internal/tmdb/movies/{tmdbId}/poster` | Téléchargement de l'affiche |
| `POST /api/internal/tmdb/movies/{tmdbId}/trailer-cache` | Prépare le cache de bande-annonce |

Le chemin complet dépend du contexte de déploiement. La spécification
[OpenAPI](../../API/Documentation/openapi.yaml) décrit ces endpoints. Les
servlets qui rendent des pages ou traitent des formulaires restent des routes
MVC, documentées séparément dans la [référence HTTP](Endpoints.md).

## Contrats et accès

### Bande-annonce

- Paramètres de requête : `film` requis et `year` facultatif.
- Réponse de succès : JSON avec `key`, `watchUrl` et `embedUrl`.
- Accès : pas de contrôle de session sur l'endpoint de l'API, comme dans le
  comportement antérieur. Le relais du principal ne doit pas être considéré
  comme une autorisation ; la route est publique.
- Le cache expire après 90 jours pour une clé trouvée et 7 jours pour un
  résultat négatif.

### Routes internes d'import

- Accès : `Authorization: Bearer <JWT>` signé en RS256 par le principal.
- Claims attendus : `iss=cynemma-main`, `aud=cynemma-api`,
  `role=EMPLOYE`, dates cohérentes et durée maximale de cinq minutes.
- Les JWT sont émis uniquement côté serveur après vérification de la session et
  du rôle employé par le servlet d'import.
- Les erreurs sont au format JSON avec le champ `erreur`; les réponses exactes
  et codes HTTP sont définis par les servlets et décrits dans OpenAPI.

Cette protection s'applique aux routes internes concernées ; elle ne protège
pas de nouvelles routes par défaut. Les appels doivent se faire sur un canal
réseau de confiance, HTTPS hors développement local, et l'accès réseau à l'API
doit être limité.

## Technologies et configuration

- Java 21, Maven, Jakarta Servlet 6.1.
- JSON via `org.json`.
- MySQL/JDBC via une DataSource JNDI dans le WAR API ; JPA/Hibernate demeure
  dans l'application principale.
- TheMovieDB appelé par le serveur API avec `TMDB_API_KEY`.
- `CYNEMMA_API_BASE_URL` et `CYNEMMA_JWT_PRIVATE_KEY` configurent le client JWT
  HTTP côté principal ; `CYNEMMA_JWT_PUBLIC_KEY` configure la validation côté
  API.

Les valeurs opérationnelles de chaque conteneur et les étapes de construction
sont décrites dans le [README du projet API](../../API/README.md).

## Limites constatées

- Il s'agit d'une API ciblée TMDB, pas d'une API REST CRUD pour les réservations,
  séances ou utilisateurs.
- La route publique de bande-annonce conserve le comportement sans session.
- Les deux WAR doivent être déployés et configurés séparément ; l'indisponibilité
  de l'API rend indisponibles les appels TMDB et l'import, mais les pages
  restantes de l'application principale sont distinctes.
- Aucun paiement, panier, hold temporaire de sièges ou générateur de salle 3D
  n'est ajouté par cette séparation.
