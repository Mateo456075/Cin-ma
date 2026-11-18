# Vue d'ensemble technique

## Ce que le projet expose

Cynemma est une application web Java déployée en WAR, structurée autour de
servlets, de JSP, de DAO et d'entités JPA/Hibernate. Dans le code examiné,
`GET /api/trailer` est le seul endpoint qui construit une réponse JSON. Les
autres routes Servlet servent des pages HTML ou traitent les formulaires de
l'application.

Cette distinction est importante : le projet n'expose pas actuellement une API
REST JSON complète pour les films, séances, utilisateurs ou réservations.

## Parcours de la route JSON

```text
Client HTTP
  -> GET /api/trailer?film=...&year=...
  -> TrailerServlet
  -> TmdbService
       -> TmdbCacheDAO / tmdb_cache (JDBC, DataSource JNDI)
       -> TMDB si le cache est absent ou expiré
  <- JSON (clé YouTube, URL de lecture et URL d'intégration)
```

La route vérifie le titre et, si fourni, l'année. Le service vérifie le cache
avant TMDB. Les clés de bande-annonce sont mises en cache 90 jours et les
résultats négatifs 7 jours. Le cache est basé sur le titre, auquel l'année est
ajoutée lorsqu'elle est fournie.

La sélection des vidéos exige `type=Trailer` et `site=YouTube`, privilégie
`official=true` et essaie les résultats français avant les résultats anglais.
Un verrou borné sérialise les cache miss portant le même titre au sein d'une
même JVM.

Références : [`TrailerServlet`](../../cynemma/src/main/java/fr/turgot/servlet/api/TrailerServlet.java),
[`TmdbService`](../../cynemma/src/main/java/fr/turgot/service/TmdbService.java),
[`TmdbCacheDAO`](../../cynemma/src/main/java/fr/turgot/dao/TmdbCacheDAO.java).

## Formats et authentification

### Endpoint JSON

- Requête : paramètres de l'URL (`film`, facultativement `year`), aucun corps
  JSON.
- Réponse : `application/json`, encodage UTF-8.
- Authentification et rôle : aucun contrôle de session ou de rôle dans la
  servlet.
- CORS : aucun en-tête CORS permissif n'est ajouté. Le navigateur applique ses
  règles d'origine ; cela ne constitue pas un contrôle d'accès pour les clients
  non navigateurs.

### Routes MVC

- Les GET servent généralement à afficher une JSP.
- Les POST reçoivent des champs de formulaire, le plus souvent
  `application/x-www-form-urlencoded`. La création manuelle d'un film utilise
  `multipart/form-data` pour l'affiche.
- Plusieurs opérations protégées vérifient le rôle de l'utilisateur en session
  et un jeton CSRF transmis comme champ `csrfToken`.
- Les réponses sont des JSP transférées en interne ou des redirections ; il n'y
  a pas de schéma JSON uniforme pour ces routes.

Les chemins et les contrôles par route sont documentés dans la
[référence des endpoints](Endpoints.md).

## Données et dépendances

Les entités de cinéma comprennent notamment `Film`, `Seance`, `Salle`,
`Rangee`, `Siege`, `Billet`, `Reservation`, `Utilisateur` et `CarteFidelite`.
Le schéma est dans [`init_database.sql`](../../cynemma/init_database.sql).

Technologies déclarées dans le
[`pom.xml`](../../cynemma/pom.xml) :

- Java avec `maven.compiler.release=21` ;
- Jakarta Servlet 6.1, JSP/JSTL ;
- Jakarta Persistence 3.1 et Hibernate 6.5 ;
- MySQL Connector/J 8.3 ;
- `org.json` 20240303 ;
- BCrypt et Jakarta Mail.

`TrailerServlet` appelle le service TMDB. Le parcours d'import employé utilise
également TMDB, mais il s'agit d'une page/formulaire MVC, pas d'un endpoint REST
JSON.

## Contexte de déploiement

Le contexte d'application n'est pas fixé par les annotations Servlet ; les
exemples sans préfixe de contexte sont donc relatifs. `web.xml` déclare un
descripteur 6.0 alors que le POM dépend de Servlet 6.1. La configuration
effective du conteneur et de la `DataSource` JNDI n'est pas présente dans le
dépôt examiné.

## Périmètre et limites

- Les routes de formulaire sont documentées comme interfaces HTTP MVC, pas
  comme API REST.
- Les statuts automatiques du conteneur lors des transferts JSP, redirections
  ou exceptions non capturées dépendent du conteneur ; la documentation ne les
  présente pas comme des réponses JSON garanties.
- Aucun test Java n'est présent dans `src/test`.
- Il n'y a pas de spécification OpenAPI ou Swagger trouvée dans le dépôt ; les
  fichiers Markdown de ce dossier constituent la documentation créée à partir
  du code.
