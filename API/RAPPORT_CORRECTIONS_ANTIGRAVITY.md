# Rapport des corrections — Cynemma et Cynemma API

Date : 18 novembre 2026  
Référence : [`AUDIT_ANTIGRAVITY.md`](AUDIT_ANTIGRAVITY.md)

## A. Résumé

- **10 corrections de code ou de documentation appliquées.**
- **4 constats du rapport initial non confirmés ou déjà obsolètes** dans l'état actuel du dépôt.
- **2 sujets nécessitent une décision métier ou des paramètres absents** : les tarifs et la géométrie des salles/balcons.
- **3 fonctionnalités restent non implémentées** : modification/suppression des séances, historique de réservations et sélection visuelle/3D des sièges.
- Les deux projets Maven restent indépendants. Les tests et le packaging des deux WAR réussissent.
- Le fonctionnement avec MySQL, TMDB et Tomcat n'a pas été vérifié en intégration.

## B. Corrections réalisées

| # | Problème | Fichiers principaux | Correction et résultat |
|---:|---|---|---|
| 1 | La clé TMDB pouvait être résolue par plusieurs mécanismes et l'absence de configuration produisait une erreur serveur générique. | `API/src/main/java/fr/turgot/api/service/TmdbService.java`, contrôleurs API, `API/README.md` | Lecture exclusivement depuis `TMDB_API_KEY`. Son absence ne bloque pas le démarrage : les routes qui ont besoin de TMDB renvoient HTTP 503 avec un message JSON explicite. `/api/health` continue à tester MySQL sans appeler TMDB. |
| 2 | Le POST interne ne vérifiait pas strictement le type JSON et son encodage ; un JSON invalide pouvait devenir une erreur 500. | `API/src/main/java/fr/turgot/api/controller/InternalTmdbServlet.java` | Accepte `application/json` sans charset ou en UTF-8, borne le corps à 4096 octets, valide l'UTF-8 strictement, exige un titre chaîne et traite les corps invalides comme HTTP 400. |
| 3 | Les identifiants de réservation étaient analysés sans validation et la relation entre film et séance n'était pas vérifiée. | `cynemma/src/main/java/fr/turgot/servlet/client/BilletServlet.java` | Validation d'entiers positifs, réponses 400/404/409 adaptées, vérification de l'existence des objets et de leur association, refus des séances déjà commencées. Le contrôle de session client et le CSRF du POST sont conservés. |
| 4 | La quantité demandée était contrôlée par rapport à la capacité déclarée plutôt qu'aux sièges réellement disponibles ; deux réservations concurrentes pouvaient choisir les mêmes sièges. | `cynemma/src/main/java/fr/turgot/dao/BilletDAO.java`, `BilletServlet.java` | La disponibilité est calculée à partir des sièges persistés. La création verrouille la ligne de séance pendant la transaction, revérifie les sièges disponibles et convertit le doublon MySQL (code 1062) en conflit métier sans masquer les autres erreurs SQL. |
| 5 | Les conversions date/heure de la séance étaient indirectes. | `cynemma/src/main/java/fr/turgot/dao/model/Seance.java` | Utilisation de `toLocalDate()` et `toLocalTime()`. |
| 6 | `/page?path=...` transférait des JSP depuis un paramètre libre. | `cynemma/src/main/java/fr/turgot/utils/PageServlet.java` | Liste blanche des seules pages publiques `Authentification`, `Creat` et `visiteur/visiteur`. Les chemins traversants, JSP internes et pages de rôles protégés sont refusés. |
| 7 | La suppression d'un film ne nettoyait pas son affiche locale. | `cynemma/src/main/java/fr/turgot/servlet/employe/DeleteFilmServlet.java`, `FilmDAO.java` | Le fichier n'est supprimé qu'après suppression réussie du film, vérification qu'aucun autre film ne référence le même chemin et validation qu'il se trouve dans le dossier `image/` de l'application. |
| 8 | La configuration de salle acceptait des valeurs négatives et des balcons IMAX malgré la règle explicite du projet. | `cynemma/src/main/java/fr/turgot/servlet/admin/CreatSalleServlet.java` | Capacité strictement positive, nombre de balcons non négatif et zéro balcon pour `IMAX`. Aucune hauteur ou géométrie de balcon n'a été inventée. |
| 9 | L'outil autonome `Salledisposiege` était embarqué dans les sources de production alors qu'il génère un ancien schéma SQL sans rapport avec les entités actuelles. | Déplacement de `cynemma/src/main/java/fr/turgot/utils/Salledisposiege.java` vers `cynemma/src/test/java/fr/turgot/utils/Salledisposiege.java` | L'outil n'est plus empaqueté dans le WAR de production ; il n'a pas été réutilisé comme générateur de coordonnées. |
| 10 | La documentation décrivait encore l'ancien placement du service/cache et des routes. | `API/Documentation/openapi.yaml`, `API/README.md`, `README.md`, `docs/API/*`, `EXPLICATION_FONCTIONNEMENT_API.md` | Documentation alignée avec le WAR API, ses six routes JSON, JWT, configuration, erreurs et déploiement. L'OpenAPI reste en version 3.0.3. |

## C. Constats vérifiés mais non corrigés car ils ne correspondent pas au code actuel

1. **Doublon `/api/trailer` dans le WAR principal : non confirmé.** Une seule servlet de relais est mappée dans `cynemma`. La servlet homonyme du projet `API/` appartient à un WAR séparé et ne crée pas de conflit de déploiement.
2. **Redirection `CreatSalleServlet` en conflit avec son mapping : non confirmé.** L'annotation, les formulaires et redirections utilisent tous `/creersalle`.
3. **Table `tmdb_cache` absente : non confirmé.** Elle est créée dans `cynemma/init_database.sql`. Ses colonnes `film_title`, `tmdb_id`, `trailer_key`, `cached_at`, sa clé primaire et la requête d'upsert de `TmdbCacheDAO` concordent ; aucune migration n'est requise.
4. **POST du cache TMDB absent de l'OpenAPI : non confirmé.** La route documentée est `POST /api/internal/tmdb/movies/{tmdbId}/trailer-cache`, telle qu'implémentée dans le servlet.

Une ancienne clé TMDB a été divulguée dans le rapport d'audit source ; sa valeur a été retirée de ce document. La clé doit être considérée comme compromise et révoquée, même si elle ne se trouve plus dans les sources applicatives actuelles.

## D. Éléments non corrigés et décisions nécessaires

### Priorité élevée — tarifs

Le formulaire ne transmet pas de tarif, ce qui évite de faire confiance au navigateur. Toutefois, ni `TypeSeance`, ni `ConfigurationSalle`, ni les tables ne définissent de grille tarifaire. `Billet.tarifApplique` et le total de réservation restent actuellement à zéro par défaut. Il faut définir les prix (par type de séance, salle, catégorie de siège ou autre règle) avant d'implémenter un recalcul métier correct ; aucun tarif n'a été inventé.

### Priorité importante — coordonnées et balcons

Les entités disposent de `Siege.coordonneeX`, `coordonneeZ`, `angleRotation` et `Rangee.elevationY`, mais aucune règle ne fixe les dimensions, unités, espacements, allées, courbure, nombre de rangées par balcon ou hauteurs. `ConfigurationSalle` laisse aussi les espacements à `NULL` et ne modélise pas les balcons individuellement. L'ancien outil `Salledisposiege` utilise des capacités arbitraires et une table `places` absente du modèle actuel : il n'est pas une source fiable à brancher.

Les coordonnées 3D et `elevation_y` restent donc non générées. La règle « IMAX sans balcon » est appliquée ; la politique des balcons pour `SCREEN_X` n'est pas déterminée dans les sources. Il faut fournir une convention géométrique et cette règle métier avant de compléter la génération.

### Fonctionnalités absentes — hors correction de bugs

- Modification et suppression des séances.
- Historique client des réservations.
- Sélection de sièges interactive ou Three.js.

Elles n'ont pas été ajoutées et ne sont pas présentées comme fonctionnelles. Le système actuel assigne des sièges disponibles automatiquement.

### Actions manuelles et tests d'intégration

- Révoquer la clé TMDB compromise et créer une nouvelle clé ; la configurer uniquement comme `TMDB_API_KEY` dans le processus du WAR API.
- Configurer `CYNEMMA_JWT_PRIVATE_KEY` côté application principale et `CYNEMMA_JWT_PUBLIC_KEY` côté API.
- Configurer une DataSource JNDI distincte dans chaque conteneur, pointant vers la base partagée, ainsi que `CYNEMMA_API_BASE_URL`.
- Déployer les deux WAR dans un conteneur compatible Jakarta Servlet 6.1 ; vérifier routes, connexion MySQL, cache, accès TMDB et réservations concurrentes.
- Configurer HTTPS hors boucle locale. Les appels avec JWT refusent maintenant HTTP distant.

## E. Tests et vérifications

| Projet / vérification | Commande ou contrôle | Résultat |
|---|---|---|
| API | `mvn -f API/pom.xml clean test` | **14 tests réussis**, aucun échec. |
| Application principale | `mvn -f cynemma/pom.xml clean test` | **23 tests réussis**, aucun échec. |
| API | `mvn -f API/pom.xml package` | **BUILD SUCCESS**, WAR généré. |
| Application principale | `mvn -f cynemma/pom.xml package` | **BUILD SUCCESS**, WAR généré. |
| OpenAPI | Analyse YAML avec Ruby et vérification des routes | Syntaxe valide, 6 chemins décrits. |
| Markdown | Vérification des liens locaux dans six documents | Aucun lien local cassé. |
| SQL / cache | Comparaison de `init_database.sql` et `TmdbCacheDAO` | Schéma existant et compatible ; pas de migration nécessaire. |
| Recherche de secret dans les sources Java/XML/SQL | Recherche de l'ancienne valeur et des alternatives de configuration | Aucune clé en clair trouvée dans le code courant ; seule la variable `TMDB_API_KEY` est lue par l'API. |

Les tests ne lancent ni Tomcat ni MySQL et ne contactent pas TMDB. Le verrou de séance, les requêtes JPQL et le nettoyage du fichier d'affiche nécessitent encore un essai d'intégration sur MySQL/Tomcat.

## F. État fonctionnel après corrections

| Domaine | État |
|---|---|
| Deux projets Maven indépendants et packaging WAR | Fonctionnel et vérifié par Maven. |
| API santé sans clé TMDB | Le code ne requiert pas TMDB pour démarrer ou vérifier MySQL ; à tester dans le conteneur. |
| Routes TMDB, JWT et validation du corps JSON | Implémentées ; tests unitaires ciblés réussis, à valider en déploiement. |
| Cache SQL `tmdb_cache` | Schéma et DAO compatibles ; nécessite MySQL/JNDI configuré. |
| Réservation par quantité avec affectation automatique | Entrées, association film/séance, disponibilité et concurrence protégées par le code ; à valider avec MySQL. |
| Montants des billets | Partiel : valeur nulle par défaut, grille tarifaire absente. |
| Géométrie, balcons et affichage 3D | Non implémentés ; paramètres et décisions métier manquants. |
| Historique et gestion de séances | Non implémentés. |

## G. Recommandations pour la suite

1. Révoquer la clé TMDB et vérifier les journaux/historiques accessibles ; la suppression d'une valeur dans le fichier courant n'efface pas les commits antérieurs.
2. Décider d'une grille tarifaire et la représenter côté serveur avant de considérer les montants de réservation comme finalisés.
3. Définir les paramètres physiques des salles et la règle `SCREEN_X`/balcons, puis concevoir le générateur déterministe en s'appuyant sur les entités existantes.
4. Tester sur MySQL les réservations concurrentes et l'upsert du cache, puis déployer les deux WAR sur Tomcat avec JNDI, JWT et HTTPS.
5. Prioriser ensuite l'historique de réservations et les opérations sur les séances comme fonctionnalités séparées, avec leurs propres critères et tests.
