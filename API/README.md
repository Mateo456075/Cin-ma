# Organisation de l'API Cynemma

## Périmètre

L'application reste un seul WAR Maven. Le seul endpoint qui expose actuellement
une réponse JSON est `GET /api/trailer`. Les autres routes Servlet de
l'application rendent des pages JSP, traitent des formulaires ou redirigent ;
elles sont documentées comme routes MVC dans [`docs/API/`](../docs/API/README.md)
et ne sont pas décrites comme endpoints REST dans OpenAPI.

## Organisation

```text
API/
├── README.md
└── Documentation/
    └── openapi.yaml

cynemma/src/main/java/fr/turgot/
├── api/
│   └── controller/
│       └── TrailerServlet.java
├── service/
│   └── TmdbService.java
├── dao/
│   └── TmdbCacheDAO.java
└── dto/
    └── TmdbMovie.java
```

- `api.controller` porte le contrat HTTP JSON : mapping Servlet, validation des
  paramètres HTTP, statuts et sérialisation des réponses.
- `service` conserve les appels TMDB et l'orchestration Cache-Aside. Il est
  partagé par l'endpoint et le parcours d'import employé ; il n'est donc pas
  déplacé dans une couche réservée à l'API.
- `dao` conserve l'accès JDBC à `tmdb_cache`.
- `dto` contient le modèle de film TMDB utilisé par l'import employé. La
  réponse de l'endpoint de bande-annonce reste un petit objet construit par le
  contrôleur ; aucune couche DTO supplémentaire n'est nécessaire pour un seul
  contrat plat.
- `API/Documentation` contient le contrat OpenAPI versionné avec le dépôt.

Les contrôleurs MVC, les modèles métier, les autres DAO et les vues restent
dans leur organisation existante. Il n'y a pas de package `Routes` séparé :
`@WebServlet` déclare le mapping HTTP dans Jakarta Servlet. Aucun middleware
global ou validateur dédié n'est présent dans l'application ; en créer pour un
seul endpoint ajouterait une couche sans responsabilité réutilisée.

## Contrat et sécurité

La route conserve son chemin et sa méthode existants. Elle accepte `film` et
`year` en paramètres de requête et renvoie du JSON. Elle n'impose pas de session
ou de rôle ; la spécification le reflète et ne déclare pas de sécurité
artificielle. Les requêtes provenant d'autres origines ne sont pas autorisées
par un en-tête CORS permissif, mais l'absence de CORS ne constitue pas une
authentification.

La route est publique et aucune limitation de débit par client n'est visible
dans le code. Des appels répétés sur des titres non présents en cache peuvent
consommer le quota TMDB ; une limitation de débit nécessite un choix de seuil
et de stratégie de déploiement, et n'est pas ajoutée dans cette restructuration.

La définition détaillée des paramètres, schémas, exemples et réponses se trouve
dans [`Documentation/openapi.yaml`](Documentation/openapi.yaml). La vue
d'ensemble des routes HTML et du seul endpoint JSON reste dans la
[documentation HTTP existante](../docs/API/README.md).

## Configuration et exécution

L'API n'est pas une application déployable indépendante. Elle utilise le
service TMDB, la `DataSource` JNDI `java:comp/env/jdbc/cynema` et la table
`tmdb_cache` de l'application principale. Les variables de configuration et
les étapes de déploiement figurent dans le [README racine](../README.md).
