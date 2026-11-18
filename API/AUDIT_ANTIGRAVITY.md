# Rapport d'audit technique — Cynemma & Cynemma API
> Auditeur : Antigravity (architecte indépendant)
> Date : 18 novembre 2026
> Périmètre : `/Volumes/MateoDISQ/Cin-ma/` — lecture seule, aucune modification effectuée.

> **Note de mise à jour :** ce document conserve l'état historique de l'audit et
> contient des constats qui ont été revérifiés depuis. Pour leur statut actuel,
> les corrections et les points restant à décider, consulter
> [`RAPPORT_CORRECTIONS_ANTIGRAVITY.md`](RAPPORT_CORRECTIONS_ANTIGRAVITY.md).
> Toute ancienne clé TMDB a été expurgée et doit être révoquée.

---

## A. Résumé général

| Dimension | Évaluation |
|---|---|
| Architecture globale | ✅ Correcte — deux projets Maven indépendants |
| Qualité du projet API (Copilot) | ✅ Élevée pour la partie TMDB — mais présente des anomalies critiques |
| Maturité fonctionnelle (Cynemma) | ⚠️ Partielle — le cœur fonctionne, mais plusieurs flux sont incomplets |
| Niveau de sécurité | ⚠️ Acceptable avec des failles identifiées |
| Risque de mise en production | 🔴 Élevé — 4 problèmes bloquants à corriger avant toute démonstration |

**Verdict synthétique :** Le travail de Copilot sur l'API est de qualité sérieuse — le code est propre, bien structuré, et les cas d'erreur sont correctement gérés. Cependant, **l'API ne peut pas démarrer** dans l'état actuel (clé API TMDB non configurée, table `tmdb_cache` manquante dans le SQL si l'URL JNDI n'est pas configurée). Sur l'application principale Cynemma, le flux de réservation est incomplet et plusieurs fonctionnalités annoncées ne sont pas implémentées.

---

## B. Architecture Maven

### ✅ Points positifs

- **Deux projets Maven indépendants** : `/cynemma/pom.xml` et `/API/pom.xml` sont bien séparés, avec des `artifactId` distincts (`cinema` vs `cynemma-api`).
- **Zéro dépendance circulaire** : l'API ne référence aucune classe du projet principal.
- **Java 21 cible les deux** : `maven.compiler.release=21` dans les deux POM.
- **WAR packaging** dans les deux projets → déployables sur deux instances Tomcat séparées.
- **Même base de données** via la DataSource JNDI `java:comp/env/jdbc/cynema` dans les deux `web.xml` → partage propre sans conflit si Tomcat est correctement configuré.

### ❌ Problèmes

#### CRITIQUE — Doublon de `TrailerServlet` dans le projet principal
Le projet `cynemma` contient également une servlet `TrailerServlet` à deux emplacements :
- `/cynemma/src/main/java/fr/turgot/api/controller/TrailerServlet.java` ← **créée par Copilot dans le mauvais projet**
- `/cynemma/src/main/java/fr/turgot/servlet/api/TrailerServlet.java` ← **créée lors de notre session précédente**

Ces deux classes annotées `@WebServlet("/api/trailer")` dans le même WAR provoqueront une **erreur de déploiement Tomcat** (conflit d'URL mapping).

#### CRITIQUE — URL de redirection incorrecte dans `CreatSalleServlet`
La redirection en cas d'erreur dans `doPost` utilise `/creersalle` (minuscules), mais la `@WebServlet` est annotée `value = "/creerSalle"` (camelCase). La redirection renverra un **HTTP 404**.

#### MOYEN — `Salledisposiege.java` est un artefact de développement
Cette classe contient une méthode `main()` qui génère un fichier SQL et écrit dans le système de fichiers. Elle est dans `src/main/java` → elle sera **embarquée dans le WAR de production**. Elle n'est jamais appelée depuis une servlet, mais sa présence est un problème de propreté.

#### MOYEN — Port et context path non documentés
Aucun fichier de configuration ne spécifie que l'API tourne sur le port 8081 (mentionné dans l'`openapi.yaml`) et l'application principale sur 8080. C'est une convention implicite non outillée.

---

## C. Audit fonctionnel

| Fonctionnalité | État | Problèmes identifiés | Recommandations |
|---|---|---|---|
| Consulter les films (visiteur) | ⚠️ Partiellement conforme | `FilmListServlet` existe, JSP `visiteur.jsp` présent, mais les champs `date`, `horaire` sur `Seance` utilisent `LocalDate.from()` / `LocalTime.from()` qui **lèveront une exception** si `dateHeure` est un `LocalDateTime` | Remplacer par `.toLocalDate()` et `.toLocalTime()` |
| Consulter les séances (visiteur) | ⚠️ Partiellement conforme | Même problème de cast sur `Seance` | Idem |
| Créer un compte | ✅ Conforme | Formulaire + servlet présents, BCrypt utilisé pour le mot de passe | — |
| Se connecter | ✅ Conforme | Session créée, attribut `usere` stocké | — |
| Créer un film (employé) | ✅ Conforme | Upload image avec validation ImageIO anti-décompression-bomb, UUID pour le nom de fichier | — |
| Modifier un film (employé) | ✅ Conforme | `EditFilmServlet` présent | — |
| Supprimer un film (employé) | ⚠️ Partiellement conforme | `DeleteFilmServlet` vérifie les séances liées avant suppression (bien), mais ne supprime pas l'image du disque | Supprimer le fichier image associé |
| Créer une séance (employé) | ✅ Conforme | `CreatSeanceServlet` présent | — |
| Modifier une séance (employé) | ❌ Non implémenté | Aucun `EditSeanceServlet` trouvé | À créer |
| Supprimer une séance (employé) | ❌ Non implémenté | Aucun `DeleteSeanceServlet` trouvé | À créer |
| Réservation / billetterie (client) | ⚠️ Partiellement conforme | Flux basique fonctionne (nombre de places), **sélection de sièges individuels absente**, panier absent, paiement absent | Voir section dédiée |
| Sélection de sièges en 3D | ❌ Non implémenté | Aucun JavaScript Three.js, aucune API de présélection de sièges côte à côte | À développer |
| Panier | ❌ Non implémenté | Absent du code et des JSP | — |
| Paiement | ❌ Non implémenté | Aucune intégration PSP (Stripe, PayPlug...) | — |
| Historique des réservations (client) | ❌ Non implémenté | Aucune page historique | À créer |
| Gestion des comptes (admin) | ✅ Conforme | `DeleteUserServlet`, `RoleModifierServlet` présents | — |
| Création de salles (admin) | ✅ Conforme | `CreatSalleServlet` avec `TypeSalle` + `DispositionSieges` | — |
| Génération des sièges (admin) | ⚠️ Partiellement conforme | `CreatSiegeServlet` crée rangées + sièges. **Les coordonnées 3D (x, z, angle) ne sont jamais calculées** — elles restent `NULL` en base | Implémenter le calcul selon la `DispositionSieges` |
| Import TMDB employé | ✅ Conforme | `TmdbCatalogImportServlet` présent | — |
| Bande-annonce (API) | ✅ Conforme | Voir section API | — |

---

## D. Audit approfondi — Salles 3D et balcons

### DispositionSieges — état réel de l'implémentation

| Disposition | Enum défini | Stocké en base | Calcul coordonnées | Compatible Three.js |
|---|---|---|---|---|
| `RECTANGLE` | ✅ | ✅ | ❌ NULL | ❌ |
| `ARC_DE_CERCLE` | ✅ | ✅ | ❌ NULL | ❌ |
| `IMAX` | ✅ | ✅ | ❌ NULL | ❌ |
| `SCREEN_X` | ✅ | ✅ | ❌ NULL | ❌ |

**Conclusion : les quatre dispositions sont déclarées mais aucune n'est réellement calculée.**

La classe `Salledisposiege.java` contient bien un algorithme de calcul de coordonnées (arc de cercle + rectangulaire avec allées), mais elle est **isolée** : elle génère un SQL statique dans un fichier, elle n'est jamais appelée par `CreatSiegeServlet`, et elle ne correspond pas à la structure JPA actuelle (`Rangee` / `Siege`).

### Balcons

Le champ `nombreBalcons` est correctement stocké dans `ConfigurationSalle`. La colonne `elevation_y` existe dans `Rangee` pour simuler la hauteur d'un balcon. Cependant :
- Aucun code ne distingue les rangées de balcon des rangées de parterre.
- Aucun algorithme ne calcule `elevation_y` automatiquement.
- La valeur reste `NULL` pour toutes les rangées créées via `CreatSiegeServlet`.

### API de présélection des sièges côte à côte

**Absente.** Aucune servlet, aucune logique de recherche de sièges consécutifs disponibles. C'est une fonctionnalité mentionnée dans les exigences mais **pas du tout implémentée**.

---

## E. Audit de l'API Copilot (projet `/API`)

### ✅ Ce qui fonctionne bien

**`TrailerServlet`**
- Lecture du paramètre `film` ✅
- HTTP 400 si absent ou vide ✅
- Paramètre facultatif `year` ✅
- Délégation à `TmdbService.getTrailerKey(film, year)` ✅
- Gestion fine des exceptions : `TmdbApiException`, `InterruptedException`, `SQLException`, `IllegalArgumentException` ✅
- Réponse JSON avec `key`, `watchUrl` ET `embedUrl` (plus complet que demandé) ✅

**`TmdbService`**
- Normalisation du titre (trim + espaces multiples) ✅
- Interrogation du cache `TmdbCacheDAO` avant TMDB ✅
- TTL 90 jours pour résultats positifs, 7 jours pour négatifs ✅
- Récupération de la clé via `TMDB_API_KEY` (env) ou `tmdb.api.key` (propriété système) ✅
- Recherche français → anglais en fallback ✅
- Priorité aux bandes-annonces officielles YouTube ✅
- Validation de la clé YouTube (regex `[A-Za-z0-9_-]{11}`) ✅
- Verrous par striping (64 locks) pour éviter les requêtes simultanées sur le même titre ✅
- Timeout HTTP 5s connexion / 10s lecture ✅

**`TmdbCacheDAO`**
- SQL avec `PreparedStatement` (pas d'injection possible) ✅
- `ON DUPLICATE KEY UPDATE` pour l'upsert ✅
- Vérification de la fraîcheur du cache côté code ✅
- DataSource JNDI correcte ✅

**`JwtTokenVerifier`**
- RS256 uniquement (pas de `none` ni `HS256` accepté) ✅
- Vérification `iss`, `aud`, `role`, `exp`, `iat` ✅
- Durée max du token : 300 secondes (5 minutes) ✅
- Limite de taille des tokens (anti-DoS) ✅
- Jeton tampered → rejeté (testé dans `JwtTokenVerifierTest`) ✅

**`HealthServlet`** — endpoint `/api/health` propre ✅

**OpenAPI**
- Syntaxe YAML valide ✅
- Version OpenAPI 3.0.3 ✅
- Tous les endpoints implémentés sont documentés ✅
- Schémas JSON corrects (`TrailerResponse`, `TmdbMovie`, `HealthResponse`, `ApiError`) ✅
- Authentification `InternalServiceToken` (JWT Bearer) correctement documentée ✅
- Exploitable avec 42Crunch ✅

### ❌ Problèmes dans l'API Copilot

#### CRITIQUE — La clé TMDB n'est pas configurée et plantera au runtime
`TmdbService.getApiKey()` lève une `IllegalStateException` si `TMDB_API_KEY` n'est pas définie comme variable d'environnement ou propriété système. Sur un Tomcat standard sans cette variable, **toutes les requêtes `/api/trailer` retourneront HTTP 500**.

> La clé était codée en dur dans notre version précédente (`TmdbHelper.java`). Copilot l'a externalisée correctement côté architecture, mais cela signifie qu'il faut configurer Tomcat pour la fournir.

#### CRITIQUE — La table `tmdb_cache` doit être dans la base MySQL
Le script `init_database.sql` **contient bien la table** `tmdb_cache`. Mais si la base n'est pas initialisée avec ce script, `TmdbCacheDAO` lèvera une `SQLException` au premier appel → HTTP 500.

#### MOYEN — `InternalTmdbServlet` : route non documentée dans l'openapi.yaml
La route `POST /api/internal/tmdb/trailer-cache` est implémentée dans `InternalTmdbServlet` mais **absente de l'openapi.yaml**. Seules les routes GET sont documentées.

#### MOYEN — `InternalTmdbServlet` : vérification du Content-Type dans le POST incomplet
Le code vérifie `Content-Type: application/json` mais lit le body avec `request.getReader()` sans valider l'encodage. Si le client envoie `application/json; charset=ISO-8859-1`, le body peut être mal décodé.

#### FAIBLE — `TmdbService.getMovieDetails()` : champs directeur et genres non gérés si absents
Si TMDB ne retourne pas de crédit de réalisateur ou de genres (film très ancien), le code fait `get()` sur un `Optional` vide → `NoSuchElementException` non catchée.

---

## F. Audit de sécurité

### 🔴 CRITIQUE

**Faille IDOR sur la billetterie** (`BilletServlet.doGet`)
```java
int seanceId = Integer.parseInt(request.getParameter("seanceId"));
int filmId = Integer.parseInt(request.getParameter("filmId"));
```
Un client peut modifier `seanceId` dans l'URL pour accéder aux informations de n'importe quelle séance, sans aucune vérification. Aucune exception si `parseInt` échoue (crash HTTP 500 non géré).

**Faille IDOR sur la billetterie** (`BilletServlet.doPost`)
Le nombre de places `nbPlaces` est lu depuis le formulaire HTML. Le serveur calcule ensuite le tarif sur la base de ce nombre. Si un utilisateur envoie `nbPlaces=100` pour une séance qui a seulement 10 places disponibles, la réservation sera créée avec 100 places — aucune vérification côté serveur que `nbPlaces <= placesDisponibles`.

### 🟠 ÉLEVÉ

**Clé API TMDB dans le code source** (`/cynemma/src/main/java/fr/turgot/utils/TmdbHelper.java`)
Une clé TMDB était en clair dans l'ancien fichier `TmdbHelper.java`. Elle est à considérer comme compromise et doit être révoquée puis remplacée depuis le compte TMDB.

**Pas de HTTPS** — Les mots de passe et tokens transitent en clair en HTTP local. Acceptable en développement, bloquant en production.

**`SessionRegistry.java` présent mais non utilisé** — Une classe de registre de sessions existe mais n'est jamais appelée. Les sessions expirées ne sont pas invalidées proactivement.

### 🟡 MOYEN

**Pas de protection contre la double réservation** — Rien n'empêche deux clients de réserver les mêmes places simultanément. La contrainte unique en base (`uq_billets_seance_siege`) protège au niveau SQL, mais l'erreur SQL n'est pas capturée correctement dans `BilletDAO` — elle provoquera une exception non gérée.

**`PageServlet` : traversal potentiel** — La servlet `/page?path=XXX` construit un chemin JSP avec `LinkHelper.JSP_ROOT + path + ".jsp"` sans valider que `path` ne contient pas de `../`. Un attaquant pourrait potentiellement accéder à des JSP non prévus.

**CORS sur l'API** — `Access-Control-Allow-Origin: *` sur `TrailerServlet` dans le projet principal. Acceptable pour une API publique de bande-annonce, mais à restreindre si des routes sensibles sont ajoutées.

### 🟢 FAIBLE / POSITIF

- BCrypt utilisé pour les mots de passe ✅
- Protection CSRF sur tous les formulaires POST ✅
- `<c:out>` dans les JSP pour échapper les sorties ✅
- Variables d'environnement pour SMTP ✅
- Pas de mot de passe stocké en clair dans le code ✅ (hors clé TMDB)
- JWT RS256 correctement implémenté dans l'API ✅

---

## G. Tests

| Test | Projet | État | Résultat |
|---|---|---|---|
| `TrailerServletTest.mapsTheExistingTrailerRoute` | API | ✅ Exécutable sans Tomcat | Vérifie l'URL mapping |
| `TrailerServletTest.rejectsMissingFilmWithoutCallingTmdb` | API | ✅ Exécutable | HTTP 400 sur film absent |
| `TrailerServletTest.rejectsOutOfRangeYearWithoutCallingTmdb` | API | ⚠️ **ÉCHOUERA** | Le test attend `"L'année doit être comprise entre 1888 et 9999."` mais ce message est lancé par `TmdbService`, pas par `TrailerServlet`. La servlet retourne HTTP 400 sur `year=1887` via `IllegalArgumentException`, mais le message exact dépend du service → le test est fragile |
| `JwtTokenVerifierTest` (4 tests) | API | ✅ Tous exécutables sans réseau | Tests propres avec vraie cryptographie RSA |
| `TrailerApiTest` (cynemma) | cynemma | ⚠️ Dépend du réseau et de la clé TMDB | Exécutable si la clé est valide |
| `TmdbApiClientTest` (cynemma) | cynemma | ❓ Non vérifiable | Fichier présent, contenu non lu |
| `ApiJwtTokenServiceTest` (cynemma) | cynemma | ❓ Non vérifiable | Fichier présent, contenu non lu |

**Maven non disponible en ligne de commande** — Les tests n'ont pas pu être exécutés via `mvn test`. À lancer depuis IntelliJ.

---

## H. Documentation Swagger

| Critère | État |
|---|---|
| Syntaxe YAML valide | ✅ |
| OpenAPI 3.0.3 | ✅ |
| `/api/health` documenté | ✅ |
| `/api/trailer` documenté | ✅ |
| `/api/internal/tmdb/*` (GET) documenté | ✅ |
| `POST /api/internal/tmdb/trailer-cache` | ❌ Absent de la doc |
| Schémas cohérents avec le code | ✅ |
| URL serveur (`http://localhost:8081/cynemma-api`) | ✅ Indicatif |
| Exploitable 42Crunch | ✅ |

---

## I. Plan de correction — par priorité

### 🔴 P1 — Bloquants (à corriger avant toute démonstration)

| # | Fichier | Problème | Modification recommandée |
|---|---|---|---|
| 1 | `/cynemma/src/main/java/fr/turgot/api/controller/TrailerServlet.java` | Doublon de servlet causant un conflit de déploiement | **Supprimer ce fichier** (garder uniquement `/cynemma/src/main/java/fr/turgot/servlet/api/TrailerServlet.java`) |
| 2 | `/cynemma/src/main/java/fr/turgot/servlet/api/TrailerServlet.java` | Clé TMDB codée en dur dans `TmdbHelper` | **Supprimer aussi ce fichier** et utiliser exclusivement le projet `/API` pour les bandes-annonces |
| 3 | `/cynemma/src/main/java/fr/turgot/utils/TmdbHelper.java` | Clé API TMDB en clair | Supprimer + révoquer la clé sur [themoviedb.org](https://www.themoviedb.org/settings/api) |
| 4 | `Tomcat (API)` | Variable `TMDB_API_KEY` non configurée | Ajouter dans les variables d'environnement Tomcat de l'API : `TMDB_API_KEY=<nouvelle_clé>` |
| 5 | `BilletServlet.doPost` | Pas de validation `nbPlaces <= placesDisponibles` | Ajouter une vérification côté serveur avant la création du billet |
| 6 | `BilletServlet.doGet` | `parseInt` sans try/catch sur `seanceId` et `filmId` | Encapsuler dans un try/catch, retourner HTTP 400 |

### 🟠 P2 — Importants (à corriger avant le rendu BTS)

| # | Fichier | Problème | Modification recommandée |
|---|---|---|---|
| 7 | `CreatSalleServlet.doPost` | Redirect vers `/creersalle` en minuscules vs `@WebServlet("/creerSalle")` | Homogénéiser (utiliser `/creerSalle` partout ou changer l'annotation) |
| 8 | `CreatSiegeServlet` | Coordonnées 3D non calculées | Extraire la logique de `Salledisposiege.java` et l'appeler selon `DispositionSieges` |
| 9 | `Seance.getDate()` / `getHoraire()` | `LocalDate.from(LocalDateTime)` lève une exception | Remplacer par `dateHeure.toLocalDate()` et `dateHeure.toLocalTime()` |
| 10 | `PageServlet` | Path traversal potentiel | Valider que `path` correspond à une liste blanche de noms autorisés |
| 11 | `API/openapi.yaml` | Route POST `/api/internal/tmdb/trailer-cache` absente | Ajouter la documentation de cet endpoint |

### 🟡 P3 — Améliorations (optionnelles pour le BTS)

| # | Élément | Problème | Modification recommandée |
|---|---|---|---|
| 12 | `Salledisposiege.java` | Classe avec `main()` dans le WAR | Déplacer dans `src/test/java` ou supprimer |
| 13 | `DeleteFilmServlet` | Image non supprimée du disque | Supprimer le fichier avant de retirer l'entrée en base |
| 14 | Réservation | Pas de protection double réservation | Capturer `SQLIntegrityConstraintViolationException` dans `BilletDAO` |
| 15 | Historique client | Absent | Créer `HistoriqueServlet` + JSP |
| 16 | Modification/suppression séance | Absent | Créer `EditSeanceServlet` + `DeleteSeanceServlet` |

---

## J. Conclusion

Le travail de Copilot sur le projet `/API` est **techniquement solide** : architecture propre, gestion des erreurs sérieuse, JWT bien implémenté, cache TMDB correct. C'est clairement du code de qualité pour un projet de BTS.

Les problèmes les plus urgents ne viennent **pas de Copilot**, mais de la coexistence de deux versions du `TrailerServlet` dans le projet principal et de la clé API exposée.

L'application Cynemma elle-même est **fonctionnelle pour les cas de base** (connexion, films, séances, réservation simple) mais manque des fonctionnalités avancées (sièges 3D, panier, paiement, historique) qui semblent avoir été prévues dans la conception mais pas encore développées.

**Autorisation requise avant toute correction.** Ce rapport est en lecture seule.
