# Plan des tests — Cynemma et Cynemma API

## Strategie

- `unit`: JUnit 5 + JUnit 4 vintage + Mockito pour les classes isolées.
- `integration`: à exécuter avec Docker/MySQL réels dès que Docker sera disponible; fallback H2 à ne pas utiliser pour les requêtes MySQL spécifiques sans validation supplémentaire.
- `api`: tests des classes Servlet et des contrats JSON; appels réseau TMDB toujours simulés.
- `security`: JWT RSA, rôles, paramètres, CSRF, parcours de réservation.
- `coverage`: JaCoCo sur les deux projets.

## Audit initial

### Cynemma API — `API/src/test/java`

| Test existant | Classe testée | Comportement | Couverture actuelle |
|---|---|---|---|
| `InternalTmdbServletTest` | `InternalTmdbServlet` | Content-Type UTF-8, JSON valide/invalide | 5 tests |
| `TrailerServletTest` | `TrailerServlet` | Mapping, film absent, année hors plage | 3 tests |
| `JwtTokenVerifierTest` | `JwtTokenVerifier` | Token employé, expiration, rôle, token altéré | 4 tests |
| `TmdbServiceTest` | `TmdbService` | Présence/absence de clé TMDB | 2 tests |

Tests ajoutés:

- `TrailerServletParametersTest`: année absente, blanche, valide, non numérique et hors bornes.
- `TmdbCacheDAOTest`: accès JNDI absent pour lecture, écriture et health-check.
- `JwtTokenVerifierExtendedTest`: mauvais émetteur, audience, ancienneté, token mal formé et algorithme interdit.

Lacunes restantes:

- Recherche TMDB réelle avec HTTP mocké (services réseau).
- Cache positif/négatif, TTL, verrouillage concurrent et SQL MySQL.
- HealthServlet avec DataSource simulée ou MySQL réel.
- Déploiement Tomcat et routes HTTP externes.

### Cynemma — `cynemma/src/test/java`

| Test existant | Classe testée | Comportement | Couverture actuelle |
|---|---|---|---|
| `TrailerServletTest` | Relais `/api/trailer` | Mapping et délégation HTTP | 3 tests |
| `BilletDAOTest` | `BilletDAO` | Conflit de siège SQL | 2 tests |
| `SeanceTest` | `Seance` | Date et horaire locaux | 2 tests |
| `ApiJwtTokenServiceTest` | `ApiJwtTokenService` | Signature JWT côté application principale | 2 tests |
| `TmdbApiClientTest` | `TmdbApiClient` | Construction des URLs/API | 4 tests |
| `CreatSalleServletTest` | `CreatSalleServlet` | Balcons rectangle/arc/IMAX | 3 tests |
| `BilletServletTest` | `BilletServlet` | Paramètres entiers positifs | 4 tests |
| `DeleteFilmServletTest` | `DeleteFilmServlet` | Suppression et relations | 1 test |
| `PageServletTest` | `PageServlet` | Liste blanche contre traversal | 2 tests |

Test ajouté:

- `SeatLayoutCalculatorTest`: toutes les dispositions, coordonnées non nulles, reproductibilité, balcons et IMAX.

Lacunes restantes:

- DAO/JPA réels sur MySQL ou H2 compatible.
- Servlet→DAO avec Mockito/Jakarta Servlet mocks.
- Réservation concurrente et double réservation.
- Transactions, suppression avec relations.
- Parcours navigateur Playwright/Tomcat.

## Corrections couvertes

| Anomalie | Etat |
|---|---|
| Double `TrailerServlet` dans le WAR Cynemma | Confirmé absent dans l'état actuel: une seule classe reste dans l'application principale |
| Secret TMDB | Aucun secret TMDB codé en dur trouvé; TMDB utilise `TMDB_API_KEY` |
| Année hors plage | Validation ajoutée directement dans `TrailerServlet` |
| Redirection `CreatSalleServlet` | Route cohérente `/creersalle` dans l'état actuel |
| Date/heure `Seance` | Utilise déjà `toLocalDate()` et `toLocalTime()` |
| Sécurité `PageServlet` | Liste blanche existante, tests présents |
| Coordonnées/balcons | Calcul déterministe ajouté à `SeatLayoutCalculator` |
| OpenAPI | À valider après chaque modification du contrat |

## Commandes

```bash
cd API && mvn clean test
cd API && mvn clean verify
cd cynemma && mvn clean test
cd cynemma && mvn clean verify
```

## Livrables non exécutés

- Docker/MySQL/Testcontainers: Docker absent.
- Tomcat: aucun conteneur actif vérifié.
- TMDB réel: aucune clé fournie dans la campagne.
- Playwright: Chromium non installé; téléchargement arrêté après timeout réseau.

