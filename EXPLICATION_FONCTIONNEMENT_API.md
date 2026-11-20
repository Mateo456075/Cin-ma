# Comprendre l’API de bandes-annonces de Cynemma

Ce document explique le code actuellement présent dans le dépôt. Il décrit la
route `GET /api/trailer` du WAR API indépendant, le service TMDB, le cache MySQL
et l'intégration de l'import employé avec l'application principale.

Les exemples de réponses ci-dessous correspondent aux formes produites par le
code. L’application n’a pas été lancée contre TMDB pendant la rédaction : les
valeurs de vidéo sont donc des exemples, pas des résultats capturés en direct.

## 1. Résumé de l’API

Le WAR API expose plusieurs endpoints JSON. Cette section se concentre sur
`GET /api/trailer` : cette route reçoit le titre d’un film, cherche une
bande-annonce YouTube avec TheMovieDB (TMDB), puis renvoie une réponse JSON.
Avant de contacter TMDB, elle cherche le résultat dans la table locale
`tmdb_cache`.

La route déclarée dans le code accepte un titre et, facultativement, une année :

```text
GET /api/trailer?film=Interstellar&year=2014
```

Le chemin complet dépend du contexte de déploiement. Dans le WAR API, l’URL peut
par exemple être :

```text
http://localhost:8080/cynemma-api/api/trailer?film=Interstellar
```
(ou sur le port `8081` si une seconde instance Tomcat est dédiée à l'API).

L’application principale conserve aussi `/api/trailer` comme relais HTTP afin
de maintenir son URL historique. Le nom exact de la route reste
**`/api/trailer`**, au singulier ; il n’y a pas de route `/api/trailers`.

## 2. Vue d’ensemble du parcours

```text
Client HTTP
    |
    | GET /api/trailer?film=Interstellar
    v
API WAR: TrailerServlet.doGet()
    |
    | valide le paramètre "film"
    v
TmdbService.getTrailerKey("Interstellar")
    |
    | 1. normalise le titre
    | 2. demande au DAO de lire le cache
    v
TmdbCacheDAO.findByFilmTitle()
    |
    +-- entrée trouvée --> retourne la clé (aucun appel TMDB)
    |
    +-- pas d’entrée --> TmdbService appelle TMDB
                           |
                           | recherche du film puis de ses vidéos
                           v
                    TmdbCacheDAO.save()
                           |
                           v
                    API TrailerServlet construit le JSON
                           |
                           v
                    Client HTTP
```

### Étapes détaillées

1. Le conteneur Servlet reçoit une requête HTTP et trouve la servlet associée au
   chemin `/api/trailer`.
2. `TrailerServlet.doGet()` lit le paramètre de requête `film`.
3. Si le paramètre est vide ou absent, la servlet renvoie HTTP 400 et ne
   contacte ni le cache ni TMDB.
4. Sinon, elle appelle `TmdbService.getTrailerKey(film, year)` ; `year` est
   facultatif.
5. Le service enlève les espaces au début et à la fin et réduit les suites
   d’espaces à un seul espace.
6. Le service demande à `TmdbCacheDAO` de chercher le titre dans
   `tmdb_cache`, par la `DataSource` JNDI `java:comp/env/jdbc/cynema`.
7. Le DAO lit aussi `cached_at`. Une clé trouvée est conservée 90 jours ; un
   résultat négatif (clé `NULL`) est conservé 7 jours. Une entrée expirée est
   traitée comme un cache miss.
8. En cas d’absence dans le cache, le service récupère la clé TMDB via
   `LocalConfiguration` (variable `TMDB_API_KEY`, propriété JVM ou fichier `cynemma.env`).
9. Il envoie une requête TMDB de recherche par titre, ajoute le filtre d’année
   si `year` est fourni, puis prend l’identifiant du premier résultat.
10. Avec cet identifiant, il demande les vidéos du film en français, puis en
    anglais si nécessaire. Pour chaque langue, il préfère une vidéo `Trailer`
    YouTube marquée `official: true` et se replie sur la première bande-annonce
    YouTube valide si aucune n’est officielle.
11. Le service enregistre dans `tmdb_cache` le titre, l’identifiant TMDB et la
    clé de vidéo. Si aucun film ou aucune bande-annonce n’est trouvé, il
    enregistre aussi ce résultat négatif.
12. Un verrou léger par titre normalisé évite que plusieurs threads lancent
    simultanément la même recherche après un cache miss. La servlet fabrique le
    JSON et le renvoie au client.

Les fichiers et méthodes principaux sont
[TrailerServlet.java](API/src/main/java/fr/turgot/api/controller/TrailerServlet.java),
[TmdbService.java](API/src/main/java/fr/turgot/api/service/TmdbService.java) et
[TmdbCacheDAO.java](API/src/main/java/fr/turgot/api/dao/TmdbCacheDAO.java).

## 3. La servlet : recevoir la requête et répondre

Fichier : [TrailerServlet.java](API/src/main/java/fr/turgot/api/controller/TrailerServlet.java)

### Annotation et classe

```java
@WebServlet("/api/trailer")
public class TrailerServlet extends HttpServlet {
```

- `@WebServlet` est une annotation Jakarta Servlet. Elle demande au conteneur
  d’associer le chemin `/api/trailer` à cette classe.
- `HttpServlet` est la classe de base utilisée pour traiter des requêtes HTTP.
- Cette classe joue le rôle de contrôleur : elle lit l’entrée HTTP, appelle le
  service, choisit le statut HTTP et écrit la réponse.

### Lecture et validation du paramètre

```java
String film = request.getParameter("film");
if (film == null || film.isBlank()) {
    writeError(response, HttpServletResponse.SC_BAD_REQUEST,
            "Paramètre 'film' manquant");
    return;
}
```

- `request` représente la requête entrante.
- `getParameter("film")` lit la valeur de `film` dans l’URL.
- `isBlank()` considère comme invalide une chaîne vide ou faite d’espaces.
- `SC_BAD_REQUEST` vaut le statut HTTP 400.
- `JSONObject` construit un objet JSON puis `toString()` le convertit en texte.
- `return` arrête immédiatement le traitement : aucun appel au service n’est
  fait pour cette requête invalide.

### Préparation de la réponse HTTP

Au début de `doGet`, la servlet définit :

```java
response.setContentType("application/json");
response.setCharacterEncoding("UTF-8");
```

Le client reçoit une réponse JSON encodée en UTF-8. Aucun en-tête CORS
permissif n’est ajouté ; le parcours prévu est Same-Origin.

### Appel au service et réponse

```java
Optional<String> key = TmdbService.getTrailerKey(film, year);
if (key.isEmpty()) {
    // la servlet renvoie une erreur JSON avec le statut 404
    return;
}
```

`Optional<String>` représente une valeur qui peut être présente ou absente.
Ici, l’absence signifie que le service n’a pas de clé de bande-annonce
disponible. Le code renvoie alors HTTP 404. Si la clé existe, la servlet crée
les URLs de lecture et d’intégration depuis cette clé.

La méthode `write(...)` écrit finalement le texte JSON dans le corps de la
réponse HTTP avec `response.getWriter()`.

## 4. Le service : logique du cache et appels TMDB

Fichier : [TmdbService.java](API/src/main/java/fr/turgot/api/service/TmdbService.java)

Cette classe orchestre le parcours : cache d’abord, TMDB ensuite. Elle n’est
pas une servlet et n’écrit pas directement dans la réponse HTTP.

### Client HTTP partagé

```java
private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();
```

`HttpClient` est fourni par Java (package `java.net.http`). Il envoie les
requêtes à TMDB. L’instance est partagée et réutilisée. Le délai de connexion
est de cinq secondes. Chaque requête HTTP reçoit aussi un délai maximal de dix
secondes dans `fetch(...)`.

### Méthode d’entrée du service

```java
public static Optional<String> getTrailerKey(String filmTitle, Integer year)
        throws TmdbApiException, InterruptedException, SQLException {
    String normalizedTitle = normalizeTitle(filmTitle);
    String cacheTitle = cacheTitle(normalizedTitle, year);
    Object lock = TRAILER_LOCKS[Math.floorMod(
            normalizedTitle.toLowerCase(Locale.ROOT).hashCode(), TRAILER_LOCKS.length)];
    synchronized (lock) {
        Optional<CacheEntry> cached =
                CACHE_DAO.findByFilmTitle(cacheTitle);
        if (cached.isPresent()) {
            String cachedKey = cached.get().trailerKey();
            if (cachedKey == null || isValidYouTubeKey(cachedKey)) {
                return Optional.ofNullable(cachedKey);
            }
        }
        // ... appels TMDB en cas de cache miss
    }
}
```

- La méthode reçoit le titre venant de la servlet.
- `year` est facultatif et sert au filtre `/search/movie`.
- `normalizeTitle(...)` vérifie le titre et normalise ses espaces.
- `cacheTitle(...)` ajoute l’année à la clé stockée lorsqu’elle a été fournie.
- Le verrou borné sérialise les cache miss simultanés d’un même titre dans la
  même JVM ; le cache est relu une fois le verrou obtenu.
- `findByFilmTitle(...)` consulte la base et traite une entrée expirée comme
  absente.
- Si une ligne existe, `Optional.ofNullable(...)` retourne sa clé, ou un
  `Optional` vide si cette ligne mémorise une absence de bande-annonce.
- Les types déclarés après `throws` signalent des erreurs que la servlet doit
  gérer : communication réseau, interruption du thread ou accès JDBC.

Le retour anticipé sur cache trouvé est la partie essentielle du pattern
**Cache-Aside** : le service vérifie le cache avant d’aller chercher la donnée
externe.

### Recherche du film chez TMDB

Le code construit une adresse de cette forme :

```text
https://api.themoviedb.org/3/search/movie
    ?api_key=...
    &language=fr-FR
    &query=Interstellar
```

Dans `searchMovieId(...)`, les valeurs sont encodées avec
`URLEncoder.encode(..., UTF_8)` avant d’être ajoutées à l’URL. Cela permet
notamment de représenter correctement les espaces et les caractères accentués.

La méthode lit le tableau JSON `results`. S’il est vide, elle retourne `null`.
Sinon, elle récupère l’identifiant du **premier** résultat. Quand `year` est
fourni à la servlet, le service ajoute aussi le filtre TMDB `year` à
`/search/movie`, ce qui aide à départager les films homonymes.

Le choix du premier résultat est un choix simple, mais ne garantit pas toujours
qu’il s’agit du bon film si plusieurs œuvres ont le même titre.

### Recherche de la vidéo

Avec l’identifiant TMDB, le service construit une URL semblable à :

```text
https://api.themoviedb.org/3/movie/123/videos
    ?api_key=...
    &language=fr-FR
```

Le nombre `123` représente ici un identifiant d’exemple. Dans le code, il vient
du résultat de la recherche TMDB.

La méthode `findTrailerKey(...)` parcourt `results` et ne retient que les objets
qui satisfont les deux conditions :

```java
"Trailer".equals(video.optString("type"))
        && "YouTube".equals(video.optString("site"))
```

`optString(...)` lit une propriété JSON sous forme de chaîne sans faire échouer
la lecture si elle manque. La méthode préfère la propriété `key` d’une vidéo
marquée `official: true`. Si aucune vidéo officielle ne correspond, elle
retourne la première clé YouTube valide. Cette clé sert à construire les URLs
de lecture et d’intégration.

### Choix de la langue

Le service demande d’abord :

```java
findTrailerKey(tmdbId, "fr-FR", apiKey)
```

Si cette recherche ne retourne pas de clé, il demande ensuite :

```java
findTrailerKey(tmdbId, "en-US", apiKey)
```

Il s’agit d’un repli français vers anglais. Dans les résultats de chaque langue,
une vidéo officielle est préférée à une vidéo non officielle.

### Envoi HTTP et traitement du statut

```java
HttpResponse<String> response =
        HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
if (response.statusCode() < 200 || response.statusCode() >= 300) {
    throw new IOException("TMDB a répondu avec le statut HTTP "
            + response.statusCode());
}
return response.body();
```

- `HttpRequest` représente la requête sortante.
- `.GET()` demande une opération HTTP GET.
- `HTTP.send(...)` envoie la requête et attend sa réponse.
- `BodyHandlers.ofString(UTF_8)` lit le corps de la réponse comme texte UTF-8.
- Un statut hors de la plage 200–299 est transformé en `TmdbApiException`.
- Un délai expiré est marqué pour permettre au contrôleur de répondre 504 ; les
  autres erreurs HTTP/réseau ou réponses JSON TMDB invalides conduisent à 502.
- Le JSON retourné par TMDB est ensuite analysé avec `JSONObject` et
  `JSONArray`.

## 5. Le DAO et le cache JDBC

Fichier : [TmdbCacheDAO.java](API/src/main/java/fr/turgot/api/dao/TmdbCacheDAO.java)

Un DAO (*Data Access Object*) regroupe les opérations d’accès aux données. Ici,
il utilise JDBC pour lire et écrire `tmdb_cache`. Ce n’est pas une entité JPA
et il n’y a pas de `TmdbCache` déclaré dans `persistence.xml`.

### Recherche d’une entrée

```sql
SELECT tmdb_id, trailer_key, cached_at
FROM tmdb_cache
WHERE film_title = ?
```

Le `?` est un paramètre préparé ; la valeur du titre est fournie séparément
avec `statement.setString(1, filmTitle)`. Cela évite de concaténer directement
le titre dans la requête SQL.

Le DAO renvoie `Optional.empty()` si aucune ligne n’existe. Si une ligne existe,
il renvoie un `CacheEntry` Java contenant `tmdbId` et `trailerKey`. Ces champs
peuvent être `null`, notamment pour mémoriser qu’aucun résultat n’a été trouvé.
Il lit aussi `cached_at` : une clé existante expire après 90 jours et une clé
`NULL` après 7 jours. Une entrée expirée est traitée comme absente, sans être
supprimée ; une recherche ultérieure peut la remplacer.

### Insertion ou mise à jour

La requête d’écriture utilise une fonctionnalité MySQL :

```sql
INSERT INTO tmdb_cache (film_title, tmdb_id, trailer_key)
VALUES (?, ?, ?)
ON DUPLICATE KEY UPDATE
    tmdb_id = VALUES(tmdb_id),
    trailer_key = VALUES(trailer_key),
    cached_at = CURRENT_TIMESTAMP
```

La clé primaire de `film_title` permet à MySQL de détecter un doublon. Une
nouvelle ligne est créée si le titre n’existe pas ; sinon les valeurs sont
mises à jour.

Les connexions, statements et résultats sont utilisés avec
`try-with-resources`. Java les ferme automatiquement à la fin du bloc, y
compris lorsqu’une exception survient.

### D’où vient la connexion JDBC ?

Le DAO cherche une `DataSource` nommée :

```text
java:comp/env/jdbc/cynema
```

`InitialContext.doLookup(...)` demande cette ressource au conteneur web.
`web.xml` déclare bien la référence `jdbc/cynema`, mais cette déclaration ne
crée pas à elle seule la connexion réelle. Le serveur doit aussi configurer et
lier une `DataSource` à ce nom.

À distinguer : [persistence.xml](cynemma/src/main/resources/META-INF/persistence.xml)
configure Hibernate/JPA avec une URL JDBC et des propriétés propres à JPA.
Le DAO du cache utilise séparément la `DataSource` JNDI. La configuration JPA
ne configure donc pas automatiquement cette connexion JNDI.

## 6. La table SQL du cache

La définition se trouve dans
[init_database.sql](cynemma/init_database.sql). Les colonnes sont :

| Colonne | Type | Rôle |
|---|---|---|
| `film_title` | `VARCHAR(255)` | Titre servant de clé primaire du cache ; l’année recherchée est ajoutée à cette valeur quand elle est fournie. |
| `tmdb_id` | `INT`, nullable | Identifiant du film chez TMDB ; `NULL` si aucun résultat n’a été trouvé. |
| `trailer_key` | `VARCHAR(64)`, nullable | Identifiant YouTube de la bande-annonce ; `NULL` si aucune vidéo adéquate n’a été trouvée. |
| `cached_at` | `TIMESTAMP` | Date de création ou de dernière mise à jour de la ligne. |

Cette table est séparée de `films` : le cache ne modifie pas le modèle métier
du cinéma. La clé étant le titre, elle n’est pas liée par clé étrangère à
l’identifiant local d’un film.

## 7. Configuration Maven et fichiers de configuration

L'API TMDB est construite comme le WAR Maven indépendant `API/`; l'application
principale `cynemma/` est un autre WAR et appelle ce service par HTTP. Chaque
projet possède son propre POM.

Le projet API utilise [pom.xml](API/pom.xml) pour déclarer ses dépendances et
son assemblage :

- `maven.compiler.release` vaut **21** : le projet est configuré pour le niveau
  de langage Java 21.
- `jakarta.servlet-api` version 6.1.0 fournit les classes Servlet ; son scope
  est `provided`, car le conteneur web doit fournir cette API à l’exécution.
- `org.json:json` version 20240303 fournit `JSONObject` et `JSONArray`.
- `mysql-connector-j` fournit le pilote JDBC MySQL.
- Le packaging est `war`, l’archive web déployable sur un conteneur compatible.

Le `web.xml` de l'API déclare une référence `jdbc/cynema` de type
`javax.sql.DataSource`. Son attribut `version` est 6.0 alors que la dépendance
Servlet du POM est 6.1. Le code source ici ne permet pas de confirmer à lui
seul la compatibilité effective du conteneur choisi avec cette combinaison.

Le service résout la configuration via la classe `LocalConfiguration` : il lit en
priorité la variable d’environnement `TMDB_API_KEY`, puis la propriété système JVM
du même nom, et enfin le fichier local `.local-secrets/cynemma.env` (ou
`${catalina.base}/conf/cynemma.env` sous Tomcat).
Cette clé est lue à la demande : son absence ne bloque pas le démarrage ni
la vérification de santé MySQL. Un cache hit pour une bande-annonce peut être
servi sans clé ; un endpoint nécessitant un appel TMDB répond HTTP 503 tant que
la clé n'est pas configurée.

## 8. Différence entre l’API Cynemma et TMDB

- **L’API Cynemma** est la route locale `/api/trailer`. Elle reçoit un titre et
  renvoie un JSON contenant une URL YouTube ou une erreur.
- **L’API TMDB** est le service externe hébergé par TheMovieDB. Cynemma lui
  envoie des requêtes HTTP pour rechercher un film et récupérer ses vidéos.
- **YouTube** héberge la vidéo. TMDB fournit à Cynemma la clé utilisée pour
  construire le lien YouTube ; le code ne télécharge pas la vidéo.

Une API externe est une application accessible à travers le réseau, avec un
contrat de requête et de réponse. Dans ce projet, Java agit comme client HTTP
de TMDB.

### Paramètres envoyés à TMDB

- `api_key` : clé d’accès TMDB. Elle reste côté serveur dans cette conception ;
  elle n’est pas reçue du navigateur comme paramètre de Cynemma.
- `language=fr-FR` ou `language=en-US` : langue demandée pour les résultats.
- `query` : titre du film à rechercher, encodé en UTF-8.
- Le chemin `/search/movie` sert à chercher le film.
- Le chemin `/movie/{id}/videos` sert à obtenir les vidéos d’un film déjà
  identifié.

## 9. Réponses JSON de Cynemma

La servlet définit le type de réponse `application/json` et l’encodage UTF-8.
Voici les formes réellement construites dans son code.

### Bande-annonce trouvée — HTTP 200

```json
{
  "key": "cleExemple1",
  "watchUrl": "https://www.youtube.com/watch?v=cleExemple1",
  "embedUrl": "https://www.youtube-nocookie.com/embed/cleExemple1"
}
```

- `key` est l’identifiant YouTube.
- `watchUrl` ouvre la vidéo sur YouTube.
- `embedUrl` est destiné à un lecteur intégré, par exemple dans une modale.

`cleExemple1` est un substitut pédagogique, pas une clé réelle capturée.
La servlet ne fixe pas explicitement le statut pour cette branche : le statut
HTTP reste normalement 200 par défaut.

### Paramètre absent — HTTP 400

```json
{
  "erreur": "Paramètre 'film' manquant"
}
```

`erreur` est une chaîne qui explique pourquoi la requête est rejetée.

### Aucun film correspondant ou aucune bande-annonce — HTTP 404

```json
{
  "erreur": "Aucune bande-annonce disponible"
}
```

`erreur` est une chaîne. L’API ne distingue pas dans ce message l’absence du
film de l’absence de vidéo.

Le service utilise la même réponse négative si TMDB ne trouve aucun film ou
s’il ne trouve pas de bande-annonce YouTube adéquate. Le client ne peut donc
pas distinguer ces deux cas à partir de cette réponse.

### Appel interrompu — HTTP 503

```json
{
  "erreur": "La recherche de bande-annonce a été interrompue"
}
```

La servlet restaure le statut d’interruption du thread avec
`Thread.currentThread().interrupt()`, puis renvoie 503.

### Erreur TMDB — HTTP 502 ou 504

```json
{
  "erreur": "Le service TMDB est temporairement indisponible"
}
```

La servlet renvoie 504 pour un délai d’attente dépassé, 502 pour les autres
erreurs réseau/HTTP ou les réponses TMDB invalides. Le détail de l’exception est
journalisé, pas envoyé au client.

### Erreur interne — HTTP 500

```json
{
  "erreur": "Erreur interne"
}
```

Cette réponse couvre notamment les erreurs SQL/JNDI et les autres erreurs
internes. Une clé TMDB absente ou remplacée par un placeholder donne HTTP 503,
avec un message indiquant que le service n'est pas configuré.

## 10. Gestion réelle des erreurs et limites

| Situation | Comportement observé dans le code |
|---|---|
| Paramètre `film` absent ou blanc | La servlet répond 400 avant l’appel du service. |
| Titre de plus de 255 caractères | `normalizeTitle(...)` lève `IllegalArgumentException`; la servlet répond 400. |
| Paramètre `year` non numérique ou hors plage 1888–9999 | La servlet ou le service rejette la requête avec HTTP 400. |
| TMDB ne retourne aucun film | Le service enregistre un résultat négatif dans le cache et renvoie un `Optional` vide ; la servlet répond 404. |
| Film présent mais aucune bande-annonce YouTube adéquate | Le service essaie d’abord `fr-FR`, puis `en-US`; si les deux échouent, il met en cache une clé nulle et la servlet répond 404. |
| Clé TMDB absente ou remplacée par un placeholder | `getApiKey()` lève `TmdbConfigurationException` lors d’un cache miss ; la servlet répond 503 et journalise l’exception. |
| Clé TMDB invalide | TMDB renvoie un statut non 2xx ; la servlet répond 502. Le code ne traduit pas spécifiquement une erreur d’authentification en 401/403. |
| Limite de requêtes TMDB ou autre statut non 2xx | La servlet répond 502. Il n’y a pas de traitement spécifique du statut 429. |
| Délai d’attente TMDB dépassé | La servlet répond 504. |
| Autre problème réseau TMDB | La servlet répond 502. Aucun nouvel essai automatique n’est codé. |
| JSON TMDB inattendu ou invalide | Le service convertit l’erreur d’analyse en erreur externe ; la servlet répond 502. |
| Table absente ou erreur SQL | Le DAO lève `SQLException`; la servlet répond 500 et journalise l’erreur. |
| JNDI `DataSource` non configurée | Le DAO transforme `NamingException` en `SQLException`; la réponse est 500. |
| Thread interrompu pendant l’appel HTTP | La servlet renvoie 503 et rétablit le drapeau d’interruption. |

Le cache expire après 7 jours pour les absences (clé `NULL`) et après 90 jours
pour les clés existantes. Il n’y a pas de tâche de nettoyage : les lignes
expirées restent dans la table jusqu’à leur remplacement.

## 11. Ce que l’analyse du dépôt confirme — et ne confirme pas

- La servlet `/api/trailer` est déclarée et le service l’appelle.
- Le WAR `API/` contient les appels HTTP à TMDB et les opérations JDBC du cache.
-   Le flux d’import du catalogue est distinct de `/api/trailer` : il est exposé
  par `/employe/tmdb/catalogue`. Le servlet principal conserve le contrôle de
  session, du rôle employé et du CSRF, puis utilise `TmdbApiClient` pour appeler
  les routes TMDB internes du WAR API avec un JWT RS256 de courte durée.
- Le dépôt contient une déclaration `resource-ref` JNDI, mais aucun fichier de
  configuration du serveur web liant effectivement la `DataSource` n’a été
  identifié dans le périmètre examiné.
- Aucune page JSP ou classe JavaScript n’appelle `/api/trailer`. La route existe,
  mais la recherche de bande-annonce de la page web n’est pas démontrée. Le
  nouveau parcours d’import TMDB utilise le service directement.
- Aucun DTO autonome de réponse REST n’a été trouvé. La servlet construit le
  JSON avec `JSONObject`. `CacheEntry` est un record interne au DAO, utilisé
  pour transporter une ligne du cache.
- Le 20 novembre 2026, une vérification manuelle de
  `GET /cynemma-api/api/trailer?film=Interstellar&year=2014` a renvoyé HTTP 200.
  Elle confirme ce parcours public dans l'environnement local à cet instant,
  mais ne valide ni tous les résultats TMDB ni les routes internes authentifiées
  et l'import complet.

## 12. Notions à connaître pour l’oral de BTS SIO SLAM

### Vocabulaire

- **API REST** : interface HTTP qui expose des ressources ou des opérations
  avec des méthodes comme GET.
- **Servlet** : composant Java exécuté par un conteneur web pour traiter des
  requêtes HTTP.
- **Contrôleur** : partie qui reçoit la requête, appelle la logique applicative
  et choisit la réponse.
- **Service** : partie qui orchestre une fonctionnalité. Ici, le service
  consulte le cache puis TMDB.
- **DAO** : classe dédiée à l’accès aux données. Ici, elle utilise JDBC pour
  le cache.
- **JDBC** : API Java bas niveau pour exécuter des requêtes SQL.
- **DataSource** : fournisseur de connexions JDBC, généralement géré par le
  serveur d’applications.
- **JNDI** : mécanisme de nommage qui permet à l’application de demander une
  ressource configurée par le conteneur.
- **JSON** : format texte structuré utilisé ici pour échanger les réponses.
- **Cache-Aside** : stratégie où l’application vérifie d’abord le cache et
  remplit celui-ci après un cache miss.
- **Cache hit** : donnée déjà présente dans le cache.
- **Cache miss** : donnée absente, qui déclenche la recherche externe.
- **Encodage URL** : transformation d’une valeur pour qu’elle soit sûre à
  insérer dans une URL.
- **Statut HTTP** : code qui indique le résultat global de la requête, par
  exemple 200, 400, 404, 502, 504 ou 500.

### Choix techniques et avantages observés

- La servlet, le service et le DAO ont des rôles séparés.
- Les requêtes SQL sont paramétrées, et les ressources JDBC sont fermées par
  `try-with-resources`.
- Le cache limite les appels TMDB répétés pour un titre déjà enregistré.
- Les réponses négatives sont également mises en cache.
- La clé TMDB est lue depuis l’environnement ou les propriétés JVM, pas
  directement depuis le code source actuel.
- Les délais de connexion et de requête HTTP sont bornés.
- Les cache miss simultanés d’un même titre sont sérialisés dans une JVM par
  un verrou parmi 64 verrous partagés.
- L’utilisation de `JSONObject` évite de construire les réponses JSON en
  concaténant manuellement des chaînes.

### Limites et améliorations possibles

1. **Clé du cache basée sur le titre.** Deux films homonymes peuvent entrer en
   conflit quand la recherche ne fournit pas d’année. Une clé fondée sur
   l’identifiant local du film serait plus stable, mais demanderait d’adapter le
   contrat de la route et le schéma.
2. **Premier résultat TMDB choisi.** L’année peut réduire les homonymies, mais
   le service prend toujours le premier résultat retourné par TMDB.
3. **Durées de cache fixes.** Les TTL sont de 7 et 90 jours et ne sont pas
   configurables.
4. **Configuration JNDI à fournir.** `web.xml` déclare la référence, mais le
   serveur doit la relier à une `DataSource` réelle.
5. **Route non reliée à l’interface dans les sources examinées.** Un appel de
   la page de séances à cette route n’a pas été trouvé.
6. **Pas de reprise automatique ni de pagination.** Le service ne réessaie pas
   après une erreur réseau et ne parcourt que le premier résultat de recherche.
7. **Le filtre année reste facultatif.** Sans `year`, une recherche par titre
   peut encore choisir le mauvais film homonyme.
8. **Cache par titre.** La recherche avec année utilise une clé de cache qui
   inclut cette année ; deux films homonymes recherchés sans année peuvent
   toujours partager une entrée.

## 13. Questions possibles à l’oral

### 1. Quelle est l’URL de la route déclarée dans le code ?

La servlet est annotée `@WebServlet("/api/trailer")`. Le paramètre obligatoire
est `film` ; `year` est facultatif. Par exemple :
`/api/trailer?film=Interstellar&year=2014`. Le contexte de l’URL complète
dépend du déploiement.

### 2. Quelle classe reçoit la requête HTTP ?

`TrailerServlet`, qui étend `HttpServlet`. L’annotation `@WebServlet` associe
le chemin au contrôleur. La méthode `doGet(...)` traite la requête GET.

### 3. Comment le titre du film est-il récupéré ?

La servlet utilise `request.getParameter("film")`. Elle vérifie ensuite que la
valeur n’est ni `null` ni blanche avant d’appeler le service.

### 4. Quel est le rôle de `TmdbService` ?

Il orchestre la recherche : normalisation du titre, lecture du cache, appels à
TMDB si nécessaire, puis enregistrement du résultat. Il ne génère pas la
réponse HTTP.

### 5. Pourquoi le code vérifie-t-il d’abord la base locale ?

C’est le principe Cache-Aside. Si une ligne existe, le service retourne sa
valeur sans appeler TMDB. Cela réduit le nombre d’appels externes pour un même
titre.

### 6. Quelle donnée sert de clé dans `tmdb_cache` ?

`film_title` est la clé primaire. Le titre normalisé est utilisé seul sans
année ; si une année est fournie, elle est ajoutée à cette clé de cache pour
séparer les résultats par année.

### 7. Comment le service trouve-t-il l’identifiant TMDB ?

Il appelle `/search/movie` avec le titre encodé, ajoute le filtre `year` s’il a
été fourni, puis prend le champ `id` du premier élément retourné par TMDB.

### 8. Comment une bande-annonce est-elle reconnue ?

Dans les résultats de `/movie/{id}/videos`, le service garde une entrée dont
`type` vaut `Trailer` et `site` vaut `YouTube`. Il privilégie les vidéos
`official: true`, puis se replie sur la première clé YouTube valide.

### 9. Comment fonctionne le repli de langue ?

Le service cherche d’abord les vidéos en `fr-FR`. Si aucune clé adéquate n’est
trouvée, il recommence en `en-US`.

### 10. Où la clé TMDB est-elle stockée et quand est-elle nécessaire ?

Le service la lit dans `TMDB_API_KEY`. Il ne la demande qu’après un cache miss ; une réponse déjà en
cache ne nécessite pas de clé.

### 11. À quoi sert `Optional<String>` dans cette méthode ?

Il indique que la méthode peut retourner une clé présente ou aucune clé. La
servlet teste `isEmpty()` pour décider entre un lien YouTube et une réponse
404.

### 12. Quel est le rôle de `TmdbCacheDAO` ?

Il exécute les requêtes SQL de lecture et d’écriture du cache. Il utilise
JDBC, une `DataSource` JNDI et des requêtes préparées ; la logique HTTP TMDB
n’est pas dans ce DAO.

### 13. Que signifie `ON DUPLICATE KEY UPDATE` ?

C’est une syntaxe MySQL. Si le titre n’existe pas, `save(...)` insère une ligne.
Si la clé primaire existe déjà, MySQL met à jour l’identifiant TMDB, la clé
YouTube et l’horodatage.

### 14. Que renvoie Cynemma si TMDB ne trouve pas de vidéo ?

Le service enregistre une ligne avec une clé de bande-annonce nulle et retourne
un `Optional` vide. La servlet répond HTTP 404 avec une propriété `erreur`.
Le même JSON est utilisé si aucun film n’a été trouvé.

### 15. Quelles configurations sont nécessaires pour déployer cette route ?

Il faut une clé TMDB dans l’environnement ou une propriété JVM, la table
`tmdb_cache` dans MySQL et une `DataSource` de conteneur liée à
`java:comp/env/jdbc/cynema`. Le `resource-ref` de `web.xml` déclare le nom,
mais le serveur doit encore configurer la ressource réelle.

## 14. Présentation orale synthétique

> « Le WAR API expose une route Jakarta Servlet déclarée sur `/api/trailer`. Elle
> reçoit un titre de film et délègue le traitement à `TmdbService`. Le service
> applique un cache-Aside : il interroge d’abord `tmdb_cache` avec un DAO JDBC.
> Si le résultat manque, il appelle TMDB pour rechercher le film, récupère son
> identifiant, puis cherche une vidéo YouTube de type bande-annonce d’abord en
> français et ensuite en anglais. Le résultat, y compris une absence de vidéo,
> est enregistré dans le cache. Enfin, la servlet construit une réponse JSON
> et renvoie un statut HTTP adapté. »

## 15. Import assisté des films actuellement en salle

Le projet contient aussi un parcours de back-office séparé pour proposer à un
employé des films TMDB à ajouter au catalogue. Ce parcours n’appelle pas la
route `/api/trailer` : le servlet MVC conserve les contrôles d'accès et appelle
les endpoints TMDB internes du WAR API avec un JWT signé côté serveur.

### Routes et contrôles d’accès

- `GET /employe/tmdb/catalogue` affiche la première page de suggestions TMDB.
  Le chemin historique `/catalogueTmdb` est également mappé sur cette servlet.
- `GET /employe/tmdb/catalogue?tmdbId=...` charge les détails et ouvre l’écran
  de relecture.
- `POST /employe/tmdb/catalogue` valide les champs et enregistre le film.

`TmdbCatalogImportServlet` vérifie le rôle `EMPLOYE` pour GET et POST. La
validation finale est un POST protégé par le jeton CSRF partagé avec le reste de
l’application. L’accès réservé aux employés correspond au contrôle appliqué
par le formulaire de création manuelle `/creerfilm`.

### Informations demandées à TMDB

`TmdbService.getNowPlayingMovies()` interroge `/movie/now_playing` avec la
langue `fr-FR` et la page 1. Il construit une liste de `TmdbMovie` contenant
l’identifiant TMDB, le titre, le résumé, la date de sortie et le chemin
d’affiche. L’endpoint est paginé par TMDB ; le code demande actuellement
uniquement la première page.

La liste ne fournit pas toutes les colonnes obligatoires de la table `films`.
Quand l’employé choisit un film, `getMovieDetails(tmdbId)` appelle
`/movie/{id}` avec `append_to_response=credits` pour préremplir aussi la durée,
les genres et le réalisateur. L’employé voit un formulaire de relecture où il
peut corriger ou compléter le titre, le réalisateur, l’année, le genre et la
durée.

La table et l’entité `Film` exigent les champs
`titre`, `realisateur`, `annee`, `genre`, `affiche` et `duree_minutes`. Le
résumé TMDB est affiché pour aider la vérification, mais il n’est pas enregistré
car l’entité `Film` n’a pas de colonne correspondante.

### Confirmation et enregistrement

À la confirmation, le contrôleur :

1. revérifie le rôle et le jeton CSRF ;
2. valide les champs obligatoires et leurs limites ;
3. recharge les détails TMDB à partir de l’identifiant envoyé ;
4. vérifie si le film existe déjà avec `FilmDAO.getFilmByTmdbId(tmdbId)` ;
5. télécharge l’affiche via la route interne de l’API, vérifie son type,
   sa taille et ses dimensions via `PosterStorage`, puis l'enregistre dans le
   répertoire de stockage externe configuré (par défaut
   `${catalina.base}/cynemma-images`, servi par `PosterImageServlet`) ;
6. crée une entité `Film` avec son identifiant `tmdb_id` et la persiste en
   réutilisant `FilmDAO.create(...)` ;
7. après une persistance réussie, demande à l’API indépendante d’actualiser le
   cache de bande-annonce (`tmdb_cache`). Cette mise à jour est distincte de la
   transaction de création du film : si elle échoue, le film reste enregistré
   et l’interface signale que le cache n’a pas été actualisé.

Si la création échoue après l’enregistrement de l’image, le contrôleur supprime
l’affiche orpheline. En cas de doublon créé concurremment, il supprime aussi
l’image téléchargée et affiche le résultat « déjà présent ».

L’identifiant TMDB est conservé à la fois dans l'entité et la table `films`
(colonne `tmdb_id` avec contrainte d'unicité `uq_films_tmdb_id` garantissant
l'idempotence des imports), et dans `tmdb_cache` associé au titre et à la clé
de bande-annonce.

La création manuelle reste un parcours distinct : la servlet
`CreatFilmServlet` et sa page `CrerFilm.jsp` ne sont pas remplacées. Elles
continuent à accepter les films d’événement ou les séances spéciales qui ne
viennent pas de TMDB. Un lien supplémentaire dans le tableau de bord employé
ouvre le nouvel import.

### Fichiers du parcours d’import

- `TmdbMovie.java` : DTO de liste et de détails reçu de TMDB.
- `TmdbService.java` : récupération de la liste, des détails, de la bande-annonce
  et du fichier image.
- `TmdbCatalogImportServlet.java` : routes GET/POST, droits, CSRF, validation,
  stockage de l’affiche et appel au DAO de films.
- `TmdbSuggestions.jsp` : présentation de la liste de nouveautés.
- `TmdbReview.jsp` : formulaire de vérification avant insertion.
- `FilmDAO.create(...)` : persistance du film local.
- `TmdbCacheDAO` : enregistrement de l’association titre, identifiant TMDB et
  clé de bande-annonce.

