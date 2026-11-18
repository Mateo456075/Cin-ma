# Documentation HTTP de Cynemma

Cette documentation décrit les routes présentes dans le code Java actuel. Elle
distingue l'endpoint JSON consommable comme une API REST des routes MVC Servlet
qui rendent des JSP ou traitent les formulaires de l'application principale.

## Contenu

- [Spécification OpenAPI 3.0](../../API/Documentation/openapi.yaml)
- [Vue d'ensemble technique](API-Overview.md)
- [Référence des routes et endpoints](Endpoints.md)
- [Erreurs, limites et points à confirmer](Errors.md)

## Inventaire

| Catégorie | Nombre | Nature |
|---|---:|---|
| Endpoints JSON du WAR `API/` | 6 | Bande-annonce, santé et intégration TMDB interne |
| Routes MVC Servlet du principal | 21 | Pages, formulaires, navigation et opérations de back-office |
| Relais historique du principal | 1 | `GET /api/trailer`, relayé vers le WAR API |

Les routes MVC ne constituent pas une API REST JSON : leurs POST reçoivent des
paramètres de formulaire et leurs réponses sont généralement des transferts JSP
ou des redirections.

## Technologies

Java 21, Jakarta Servlet 6.1, JSP/JSTL, JPA/Hibernate, MySQL/JDBC, HTTP Client,
JWT RS256 et `org.json`. Les dépendances sont définies dans le
[`pom.xml` principal](../../cynemma/pom.xml) et le
[`pom.xml` de l'API](../../API/pom.xml).

## Utilisation

L'URL complète dépend du contexte de déploiement. Par exemple, avec le contexte
`/cynemma-api`, l'endpoint JSON indépendant est :

```http
GET /cynemma-api/api/trailer?film=Interstellar&year=2014
Accept: application/json
```

Le paramètre `year` est facultatif. Voir la
[référence détaillée](Endpoints.md#api-json--war-api). Dans l'application
principale, le même chemin `/api/trailer` est conservé comme relais pour les
clients existants.

Les exemples de formulaires dans la référence illustrent le format attendu par
les servlets ; ils ne transforment pas ces routes en endpoints JSON.

## Sources examinées

Les annotations `@WebServlet`, les méthodes `doGet`/`doPost`, leurs paramètres,
les contrôles de session/CSRF, les DAO/services appelés, les vues JSP, le POM,
`web.xml`, `persistence.xml` et le schéma SQL ont été consultés. La
[documentation fonctionnelle TMDB](../../EXPLICATION_FONCTIONNEMENT_API.md)
décrit plus longuement la logique de bande-annonce et de cache.
