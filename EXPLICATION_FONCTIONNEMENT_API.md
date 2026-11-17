# Comprendre l’API de bandes-annonces de Cynemma

Ce document explique le code actuellement présent dans le dépôt. Il décrit la
route `GET /api/trailer`, le service TMDB, le cache MySQL et les réponses
fabriquées par l’application.

Les exemples de réponses ci-dessous correspondent aux formes produites par le
code. L’application n’a pas été lancée contre TMDB pendant la rédaction : les
valeurs de vidéo sont donc des exemples, pas des résultats capturés en direct.

## 1. Résumé de l’API

L’application expose une route qui reçoit le titre d’un film, cherche une
bande-annonce YouTube avec TheMovieDB (TMDB), puis renvoie une réponse JSON.
Avant de contacter TMDB, elle cherche le résultat dans la table locale
`tmdb_cache`.

La route déclarée dans le code est :

```text
GET /api/trailer?film=Interstellar
```

Le chemin complet dépend du contexte de déploiement. Si l’application est
déployée sous le contexte `cinema`, l’URL peut par exemple être :

```text
http://localhost:8080/cinema/api/trailer?film=Interstellar
```

Le nom exact dans l’annotation est **`/api/trailer`**, au singulier. Il n’y a
pas de route `/api/trailers` dans les fichiers analysés.

## 2. Vue d’ensemble du parcours

```text
Client HTTP
    |
    | GET /api/trailer?film=Interstellar
    v
TrailerServlet.doGet()
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
                    TrailerServlet construit le JSON
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
4. Sinon, elle appelle `TmdbService.getTrailerKey(film)`.
5. Le service enlève les espaces au début et à la fin et réduit les suites
   d’espaces à un seul espace.
6. Le service demande à `TmdbCacheDAO` de chercher le titre dans
   `tmdb_cache`, par la `DataSource` JNDI `java:comp/env/jdbc/cynema`.
7. Si une ligne existe, le service retourne sa clé de bande-annonce. Il ne lit
   alors pas la clé TMDB et ne fait aucun appel réseau à TMDB.
8. En cas d’absence dans le cache, le service récupère la clé TMDB dans la
   variable d’environnement `TMDB_API_KEY`, ou dans la propriété JVM
   `tmdb.api.key` si la variable n’est pas définie.
9. Il envoie une requête TMDB de recherche par titre et prend l’identifiant du
   premier résultat.
10. Avec cet identifiant, il demande les vidéos du film en français. S’il ne
    trouve pas de vidéo de type `Trailer` hébergée sur `YouTube`, il recommence
    la recherche des vidéos en anglais.
11. Le service enregistre dans `tmdb_cache` le titre, l’identifiant TMDB et la
    clé de vidéo. Si aucun film ou aucune bande-annonce n’est trouvé, il
    enregistre aussi ce résultat négatif.
12. La servlet fabrique le JSON et le renvoie au client.

Les fichiers et méthodes principaux sont
[TrailerServlet.java](cynemma/src/main/java/fr/turgot/servlet/api/TrailerServlet.java),
[TmdbService.java](cynemma/src/main/java/fr/turgot/service/TmdbService.java) et
[TmdbCacheDAO.java](cynemma/src/main/java/fr/turgot/dao/TmdbCacheDAO.java).

## 3. La servlet : recevoir la requête et répondre

Fichier : [TrailerServlet.java](cynemma/src/main/java/fr/turgot/servlet/api/TrailerServlet.java)

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
    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    write(response, new JSONObject()
            .put("erreur", "Paramètre 'film' manquant").toString());
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
response.setHeader("Access-Control-Allow-Origin", "*");
```

Le client reçoit une réponse de type JSON encodée en UTF-8. L’en-tête CORS
autorise les navigateurs de toutes les origines à lire la réponse. Cette
autorisation est large ; elle n’authentifie pas les utilisateurs et ne protège
pas la route.

### Appel au service et réponse

```java
Optional<String> key = TmdbService.getTrailerKey(film);
if (key.isEmpty()) {
    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    // la servlet renvoie trailer: null et un message
    return;
}
```

`Optional<String>` représente une valeur qui peut être présente ou absente.
Ici, l’absence signifie que le service n’a pas de clé de bande-annonce
disponible. Le code renvoie alors HTTP 404. Si la clé existe, la servlet crée
l’URL YouTube et la place dans le JSON.

La méthode `write(...)` écrit finalement le texte JSON dans le corps de la
réponse HTTP avec `response.getWriter()`.

## 4. Le service : logique du cache et appels TMDB

Fichier : [TmdbService.java](cynemma/src/main/java/fr/turgot/service/TmdbService.java)

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
public static Optional<String> getTrailerKey(String filmTitle)
        throws IOException, InterruptedException, SQLException {
    String normalizedTitle = normalizeTitle(filmTitle);
    Optional<CacheEntry> cached =
            CACHE_DAO.findByFilmTitle(normalizedTitle);
    if (cached.isPresent()) {
        return Optional.ofNullable(cached.get().trailerKey());
    }
    // ... appels TMDB en cas de cache miss
}
```

- La méthode reçoit le titre venant de la servlet.
- `normalizeTitle(...)` vérifie le titre et normalise ses espaces.
- `findByFilmTitle(...)` consulte la base.
- `Optional.empty()` renvoyé par le DAO signifie qu’aucune ligne de cache
  n’existe.
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
Sinon, elle récupère l’identifiant du **premier** résultat :

```java
JSONArray results = new JSONObject(fetch(url)).getJSONArray("results");
if (results.isEmpty()) {
    return null;
}
return results.getJSONObject(0).getInt("id");
```

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
la lecture si elle manque. La méthode retourne la propriété `key` de la
première vidéo correspondante. Cette clé permet de construire le lien
`https://www.youtube.com/watch?v=...`.

### Choix de la langue

Le service demande d’abord :

```java
findTrailerKey(tmdbId, "fr-FR", apiKey)
```

Si cette recherche ne retourne pas de clé, il demande ensuite :

```java
findTrailerKey(tmdbId, "en-US", apiKey)
```

Il s’agit d’un repli français vers anglais. Le service ne compare pas plusieurs
bandes-annonces entre elles : il conserve la première vidéo qui est à la fois
de type `Trailer` et de site `YouTube`.

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
- Un statut hors de la plage 200–299 est transformé en `IOException`.
- Le JSON retourné par TMDB est ensuite analysé avec `JSONObject` et
  `JSONArray`.

## 5. Le DAO et le cache JDBC

Fichier : [TmdbCacheDAO.java](cynemma/src/main/java/fr/turgot/dao/TmdbCacheDAO.java)

Un DAO (*Data Access Object*) regroupe les opérations d’accès aux données. Ici,
il utilise JDBC pour lire et écrire `tmdb_cache`. Ce n’est pas une entité JPA
et il n’y a pas de `TmdbCache` déclaré dans `persistence.xml`.

### Recherche d’une entrée

```sql
SELECT tmdb_id, trailer_key
FROM tmdb_cache
WHERE film_title = ?
```

Le `?` est un paramètre préparé ; la valeur du titre est fournie séparément
avec `statement.setString(1, filmTitle)`. Cela évite de concaténer directement
le titre dans la requête SQL.

Le DAO renvoie `Optional.empty()` si aucune ligne n’existe. Si une ligne existe,
il renvoie un `CacheEntry` Java contenant `tmdbId` et `trailerKey`. Ces champs
peuvent être `null`, notamment pour mémoriser qu’aucun résultat n’a été trouvé.

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
| `film_title` | `VARCHAR(255)` | Titre servant de clé primaire du cache. |
| `tmdb_id` | `INT`, nullable | Identifiant du film chez TMDB ; `NULL` si aucun résultat n’a été trouvé. |
| `trailer_key` | `VARCHAR(64)`, nullable | Identifiant YouTube de la bande-annonce ; `NULL` si aucune vidéo adéquate n’a été trouvée. |
| `cached_at` | `TIMESTAMP` | Date de création ou de dernière mise à jour de la ligne. |

Cette table est séparée de `films` : le cache ne modifie pas le modèle métier
du cinéma. La clé étant le titre, elle n’est pas liée par clé étrangère à
l’identifiant local d’un film.

## 7. Configuration Maven et fichiers de configuration

Le projet utilise [pom.xml](cynemma/pom.xml) pour déclarer ses dépendances et
son assemblage :

- `maven.compiler.release` vaut **21** : le projet est configuré pour le niveau
  de langage Java 21.
- `jakarta.servlet-api` version 6.1.0 fournit les classes Servlet ; son scope
  est `provided`, car le conteneur web doit fournir cette API à l’exécution.
- `org.json:json` version 20240303 fournit `JSONObject` et `JSONArray`.
- `mysql-connector-j` fournit le pilote JDBC MySQL.
- Le packaging est `war`, l’archive web déployable sur un conteneur compatible.

`web.xml` déclare une référence `jdbc/cynema` de type
`javax.sql.DataSource`. Son attribut `version` est 6.0 alors que la dépendance
Servlet du POM est 6.1. Le code source ici ne permet pas de confirmer à lui
seul la compatibilité effective du conteneur choisi avec cette combinaison.

Le service ne lit aucun fichier `.env` ou fichier de propriétés dans les
sources examinées. Il lit directement :

1. la variable système `TMDB_API_KEY` ;
2. sinon, la propriété JVM `tmdb.api.key`.

Si aucune des deux n’est renseignée, `getApiKey()` lève une
`IllegalStateException`. Toutefois, cette méthode n’est appelée qu’en cas de
cache miss : une réponse déjà en cache peut être servie sans clé TMDB.

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
  "trailer": "https://www.youtube.com/watch?v=cleExemple"
}
```

- `trailer` est une chaîne (`String`).
- Sa valeur est construite en ajoutant la clé trouvée à l’URL YouTube.
- Le client peut utiliser ce lien pour ouvrir la vidéo ou, après vérification
  des règles YouTube et de sécurité de son interface, l’intégrer.

Le nom `cleExemple` est un substitut pédagogique, pas une clé réelle capturée.
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
  "trailer": null,
  "message": "Aucune bande-annonce disponible"
}
```

- `trailer` est la valeur JSON `null`, pas la chaîne `"null"`.
- `message` est une chaîne.

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

### Autre erreur — HTTP 500

```json
{
  "erreur": "Erreur interne"
}
```

Le détail de l’exception n’est pas envoyé au client. La servlet l’écrit dans le
journal du conteneur avec `getServletContext().log(...)`.

## 10. Gestion réelle des erreurs et limites

| Situation | Comportement observé dans le code |
|---|---|
| Paramètre `film` absent ou blanc | La servlet répond 400 avant l’appel du service. |
| Titre de plus de 255 caractères | `normalizeTitle(...)` lève `IllegalArgumentException`; la servlet répond 500. |
| TMDB ne retourne aucun film | Le service enregistre un résultat négatif dans le cache et renvoie un `Optional` vide ; la servlet répond 404. |
| Film présent mais aucune bande-annonce YouTube adéquate | Le service essaie d’abord `fr-FR`, puis `en-US`; si les deux échouent, il met en cache une clé nulle et la servlet répond 404. |
| Clé TMDB absente | `getApiKey()` lève `IllegalStateException` lors d’un cache miss ; la servlet répond 500 et journalise l’exception. |
| Clé TMDB invalide | TMDB renverra probablement un statut non 2xx ; `fetch(...)` lève une `IOException`, puis la servlet répond 500. Le code ne traduit pas spécifiquement une erreur d’authentification en 401/403. |
| Limite de requêtes TMDB ou autre statut non 2xx | `fetch(...)` lève une `IOException`; la servlet répond 500. Il n’y a pas de traitement spécifique du statut 429. |
| Délai réseau ou indisponibilité de TMDB | Une exception réseau remonte au contrôleur ; la réponse est normalement 500. Aucun nouvel essai automatique n’est codé. |
| JSON TMDB inattendu ou invalide | L’analyse par `JSONObject`/`JSONArray` échoue ; l’exception remonte et la servlet répond 500. |
| Table absente ou erreur SQL | Le DAO lève `SQLException`; la servlet répond 500 et journalise l’erreur. |
| JNDI `DataSource` non configurée | Le DAO transforme `NamingException` en `SQLException`; la réponse est 500. |
| Thread interrompu pendant l’appel HTTP | La servlet renvoie 503 et rétablit le drapeau d’interruption. |

Le code ne contient pas de cache à durée de vie limitée. `cached_at` est
enregistré, mais aucune méthode ne l’utilise pour invalider une entrée. Une
réponse négative peut donc rester en cache indéfiniment, même si une
bande-annonce est ajoutée ultérieurement à TMDB.

## 11. Ce que l’analyse du dépôt confirme — et ne confirme pas

- La servlet `/api/trailer` est déclarée et le service l’appelle.
- Le service contient les appels HTTP à TMDB et le DAO contient les opérations
  JDBC du cache.
- Le flux d’import du catalogue est distinct de `/api/trailer` : il est exposé
  par `/employe/tmdb/catalogue` et passe directement par `TmdbService`.
- Le dépôt contient une déclaration `resource-ref` JNDI, mais aucun fichier de
  configuration du serveur web liant effectivement la `DataSource` n’a été
  identifié dans le périmètre examiné.
- Aucune page JSP ou classe JavaScript n’appelle `/api/trailer`. La route existe,
  mais la recherche de bande-annonce de la page web n’est pas démontrée. Le
  nouveau parcours d’import TMDB utilise le service directement.
- Aucun DTO autonome de réponse REST n’a été trouvé. La servlet construit le
  JSON avec `JSONObject`. `CacheEntry` est un record interne au DAO, utilisé
  pour transporter une ligne du cache.
- Aucun appel réel à TMDB n’a été effectué pour ce document. Les exemples de
  JSON montrent le format construit par le code, pas un résultat observé en
  production.

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
  exemple 200, 400, 404 ou 500.

### Choix techniques et avantages observés

- La servlet, le service et le DAO ont des rôles séparés.
- Les requêtes SQL sont paramétrées, et les ressources JDBC sont fermées par
  `try-with-resources`.
- Le cache limite les appels TMDB répétés pour un titre déjà enregistré.
- Les réponses négatives sont également mises en cache.
- La clé TMDB est lue depuis l’environnement ou les propriétés JVM, pas
  directement depuis le code source actuel.
- Les délais de connexion et de requête HTTP sont bornés.
- L’utilisation de `JSONObject` évite de construire les réponses JSON en
  concaténant manuellement des chaînes.

### Limites et améliorations possibles

1. **Clé du cache basée sur le titre.** Deux films homonymes peuvent entrer en
   conflit. Une clé fondée sur l’identifiant local du film serait plus stable,
   mais demanderait d’adapter le contrat de la route et le schéma.
2. **Premier résultat TMDB choisi.** Le code ne compare ni l’année ni d’autres
   critères pour confirmer qu’il s’agit du bon film.
3. **Pas d’expiration du cache.** La colonne `cached_at` pourrait servir à
   définir une durée de validité ou une stratégie d’invalidation.
4. **Erreurs externes peu différenciées.** Le contrôleur répond généralement
   500 aux erreurs TMDB, y compris aux statuts qui pourraient être gérés
   séparément.
5. **Configuration JNDI à fournir.** `web.xml` déclare la référence, mais le
   serveur doit la relier à une `DataSource` réelle.
6. **Route non reliée à l’interface dans les sources examinées.** Un appel de
   la page de séances à cette route n’a pas été trouvé.
7. **CORS ouvert à toutes les origines.** `Access-Control-Allow-Origin: *` est
   large ; la valeur devrait correspondre au besoin réel de déploiement.
8. **Pas de reprise automatique ni de pagination.** Le service ne réessaie pas
   après une erreur réseau et ne parcourt que le premier résultat de recherche.
9. **Parallélisme des cache miss.** Deux requêtes simultanées pour le même
   nouveau titre peuvent chacune appeler TMDB avant que l’une ait enregistré
   le résultat.

## 13. Questions possibles à l’oral

### 1. Quelle est l’URL de la route déclarée dans le code ?

La servlet est annotée `@WebServlet("/api/trailer")`. Le paramètre attendu est
`film`, par exemple `/api/trailer?film=Interstellar`. Le contexte de l’URL
complète dépend du déploiement.

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

`film_title` est la clé primaire. Le titre est normalisé côté Java avant la
recherche. Ce choix est simple, mais deux films avec le même titre peuvent être
confondus.

### 7. Comment le service trouve-t-il l’identifiant TMDB ?

Il appelle `/search/movie` avec le titre encodé, analyse `results` et prend le
champ `id` du premier élément. Il ne fait pas de vérification supplémentaire
sur l’année ou le réalisateur.

### 8. Comment une bande-annonce est-elle reconnue ?

Dans les résultats de `/movie/{id}/videos`, le service garde une entrée dont
`type` vaut `Trailer` et `site` vaut `YouTube`. Il retourne son champ `key`.

### 9. Comment fonctionne le repli de langue ?

Le service cherche d’abord les vidéos en `fr-FR`. Si aucune clé adéquate n’est
trouvée, il recommence en `en-US`.

### 10. Où la clé TMDB est-elle stockée et quand est-elle nécessaire ?

Le service la lit dans `TMDB_API_KEY`, ou à défaut dans la propriété JVM
`tmdb.api.key`. Il ne la demande qu’après un cache miss ; une réponse déjà en
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
un `Optional` vide. La servlet répond HTTP 404 avec `trailer: null` et un
message. Le même JSON est utilisé si aucun film n’a été trouvé.

### 15. Quelles configurations sont nécessaires pour déployer cette route ?

Il faut une clé TMDB dans l’environnement ou une propriété JVM, la table
`tmdb_cache` dans MySQL et une `DataSource` de conteneur liée à
`java:comp/env/jdbc/cynema`. Le `resource-ref` de `web.xml` déclare le nom,
mais le serveur doit encore configurer la ressource réelle.

## 14. Présentation orale synthétique

> « Ma route REST est une servlet Jakarta déclarée sur `/api/trailer`. Elle
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
route `/api/trailer` : il utilise directement `TmdbService`.

### Routes et contrôles d’accès

- `GET /employe/tmdb/catalogue` affiche la première page de suggestions TMDB.
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
4. appelle `getTrailerKeyForMovie(tmdbId, titre)` pour rechercher en français
   puis en anglais et enregistrer l’identifiant et la clé (ou une clé nulle)
   dans `tmdb_cache` ;
5. télécharge l’affiche depuis le serveur d’images TMDB, vérifie son type,
   sa taille et ses dimensions, puis la convertit en PNG sous `image/` ;
6. crée une entité `Film` et la persiste en réutilisant `FilmDAO.create(...)`.

L’identifiant TMDB n’est pas ajouté à l’entité ni à la table `films` : il est
conservé avec la clé de bande-annonce dans `tmdb_cache`, associée au titre
validé. Le cache actuel ayant le titre comme clé primaire, il ne peut pas
représenter plusieurs identifiants TMDB pour des films portant le même titre.

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

