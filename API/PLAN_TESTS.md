# État et plan des tests — Cynemma et Cynemma API

## Strategie

- `unit`: JUnit 5 et JUnit 4 via Vintage ; les tests emploient des mocks ou des
  doublures selon la classe testée.
- `integration`: aucun test d'intégration n'est configuré. Les prochains tests
  doivent utiliser MySQL réel ou Testcontainers ; aucun fallback H2 n'est prévu
  pour valider les requêtes spécifiques à MySQL.
- `api`: tests des classes Servlet et des contrats JSON; appels réseau TMDB toujours simulés.
- `security`: JWT RSA, rôles, paramètres, CSRF, parcours de réservation.
- `coverage`: JaCoCo sur les deux projets.

## Tests automatisés présents

Dernière exécution documentée : 20 novembre 2026. `mvn test` a exécuté 25 tests
dans `API/` et 42 dans `cynemma/`, sans échec. Les tests ne constituent pas une
validation d'intégration avec MySQL, TMDB ou le parcours navigateur complet.

### Cynemma API — `API/src/test/java`

| Test existant | Classe testée | Comportement | Couverture actuelle |
|---|---|---|---|
| `InternalTmdbServletTest` | `InternalTmdbServlet` | Type de contenu JSON UTF-8 et analyse JSON valide/invalide | 5 tests |
| `TrailerServletTest` | `TrailerServlet` | Mapping, film absent, année hors plage | 3 tests |
| `JwtTokenVerifierTest` | `JwtTokenVerifier` | Token employé, expiration, rôle, token altéré | 4 tests |
| `TmdbServiceTest` | `TmdbService` | Validation de clé présente, absente ou placeholder | 3 tests |

Tests ajoutés:

- `TrailerServletParametersTest`: année absente, blanche, valide, non numérique et hors bornes.
- `TmdbCacheDAOTest`: accès JNDI absent pour lecture, écriture et health-check.
- `JwtTokenVerifierExtendedTest`: mauvais émetteur, audience, ancienneté, token mal formé et algorithme interdit.
- `LocalConfigurationTest`: hiérarchie de configuration (variables d'environnement, propriétés JVM, `.local-secrets/cynemma.env`, format et parsing).

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
| `TmdbApiClientTest` | `TmdbApiClient` | Construction d'URL et validation des types MIME d'image | 6 tests |
| `CreatSalleServletTest` | `CreatSalleServlet` | Balcons rectangle/arc/IMAX | 3 tests |
| `BilletServletTest` | `BilletServlet` | Paramètres entiers positifs | 4 tests |
| `DeleteFilmServletTest` | `DeleteFilmServlet` | Suppression et relations | 1 test |
| `PageServletTest` | `PageServlet` | Liste blanche contre traversal | 2 tests |

Tests ajoutés:

- `SeatLayoutCalculatorTest`: toutes les dispositions, coordonnées non nulles, reproductibilité, balcons et IMAX.
- `LocalConfigurationTest`: résolution de configuration côté application principale (variables d'environnement, propriétés JVM, `cynemma.env`).
- `PosterStorageTest`: validation des formats d'image, dimensions, corruption et persistance externe des affiches.
- `EditFilmServletTest`: conservation du `tmdbId` lors de la mise à jour d'un film, à l'aide d'une doublure DAO.
- `TmdbCatalogImportServletTest`: refus d'un utilisateur non employé ou d'un CSRF invalide, détection des doublons, persistance avec affiche, échec du cache et nettoyage de l'affiche si la création échoue.
- `TmdbCatalogImportServletMappingTest`: mapping et contrôle d'accès de la servlet d'import du catalogue.

Lacunes restantes:

- DAO/JPA sur une base MySQL réelle.
- Intégration Servlet/DAO avec JPA et MySQL réels ; les tests d'import utilisent des doublures.
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
mvn -f API/pom.xml clean test
mvn -f API/pom.xml clean verify
mvn -f cynemma/pom.xml clean test
mvn -f cynemma/pom.xml clean verify
```

## Vérifications d'intégration non couvertes

- La suite automatisée ne démarre pas MySQL/Testcontainers et n'exerce pas les
  transactions JPA sur une base réelle.
- Le 20 novembre 2026, Tomcat a répondu `200` à `/cynemma-api/api/health` et à
  une requête publique `/cynemma-api/api/trailer`. Cela ne valide pas les
  endpoints internes authentifiés ni le parcours complet d'import.
- Aucun test navigateur Playwright n'est présent dans le dépôt.

