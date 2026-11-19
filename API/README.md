# Projet API Cynemma

## Architecture

`API/` est un projet Maven autonome qui produit `cynemma-api.war`. Il se déploie
comme application Web séparée, dans un contexte distinct du même conteneur
Servlet que l'application principale (ou dans un conteneur séparé si
l'exploitation le nécessite). Ce n'est pas une deuxième base de données ni un
microservice CRUD indépendant du domaine. L'application principale conserve
les JSP, les sessions utilisateur et la persistance des films importés.

```text
API/
├── pom.xml
├── README.md
├── Documentation/openapi.yaml
└── src/
    ├── main/java/fr/turgot/api/
    │   ├── controller/       # Servlets HTTP/JSON et validation de protocole
    │   ├── dao/              # JDBC vers le cache partagé
    │   ├── dto/              # Modèles TMDB exposés à l'application principale
    │   ├── security/         # Vérification des JWT signés par Cynemma
    │   └── service/          # Appels TMDB, Cache-Aside et sélection de bande-annonce
    ├── main/webapp/WEB-INF/web.xml
    └── test/java/            # Tests du contrat Servlet
```

Le service TMDB et le DAO de cache ont été déplacés hors de `cynemma/` : l'API
est leur propriétaire unique. Le flux employé conserve ses pages et validations,
mais appelle l'API par HTTP depuis le serveur principal. Le contrôleur principal
`GET /api/trailer` est conservé comme relais HTTP vers le nouveau serveur pour
garder l'URL historique de l'application.

## Endpoints implémentés

| Méthode et route dans le WAR API | Accès | Rôle |
|---|---|---|
| `GET /api/health` | Public | Vérifie la connexion MySQL |
| `GET /api/trailer?film=...&year=...` | Public | Recherche cache-aware de bande-annonce |
| `GET /api/internal/tmdb/now-playing` | JWT employé | Suggestions pour l'import |
| `GET /api/internal/tmdb/movies/{tmdbId}` | JWT employé | Détails préremplis |
| `GET /api/internal/tmdb/movies/{tmdbId}/poster` | JWT employé | Téléchargement de l'affiche |
| `POST /api/internal/tmdb/movies/{tmdbId}/trailer-cache` | JWT employé | Prépare le cache de bande-annonce |

L'OpenAPI [Documentation/openapi.yaml](Documentation/openapi.yaml) décrit ces
routes uniquement. Les routes HTML/MVC existantes sont inventoriées dans la
[documentation HTTP générale](../docs/API/README.md). Les fonctionnalités
réservation avancée, hold temporaire, panier, tarifs, produits, paiement et
historique ne sont pas déclarées : leur modèle métier n'existe pas encore.

## Authentification entre les projets

La connexion de l'employé reste celle de l'application principale. Après son
contrôle de session et de rôle, le serveur principal émet un JWT RS256 de cinq
minutes, signé avec la clé privée. L'API vérifie la signature avec la clé
publique et exige l'émetteur `cynemma-main`, l'audience `cynemma-api` et le rôle
`EMPLOYE`. Le JWT est transmis uniquement dans l'en-tête
`Authorization: Bearer ...` entre serveurs ; il n'est pas exposé au navigateur.

Générer une paire de clés hors du dépôt, dans un emplacement protégé :

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out cynemma-private.pem
openssl pkey -in cynemma-private.pem -pubout -out cynemma-public.pem
openssl pkey -in cynemma-private.pem -outform DER | base64 | tr -d '\n'
openssl pkey -pubin -in cynemma-public.pem -outform DER | base64 | tr -d '\n'
```

Les deux dernières commandes affichent les valeurs encodées à transmettre au
gestionnaire de secrets de chaque processus. Fournir le DER PKCS#8 de la clé
privée encodé en Base64 au processus Java principal dans
`CYNEMMA_JWT_PRIVATE_KEY`. Fournir le DER X.509 de la clé publique encodé en
Base64 au processus Java API dans
`CYNEMMA_JWT_PUBLIC_KEY`. Garder la clé privée uniquement côté serveur principal ;
ne jamais versionner les clés ou les jetons.

Cette fédération concerne les appels serveur-à-serveur de l'import TMDB. Les
endpoints de gestion des réservations, salles et utilisateurs n'ont pas encore
été développés ; aucune autorisation future n'est implicitement couverte par
ce seul rôle.

## Configuration et base de données

L'API utilise la même base MySQL que Cynemma, mais chaque conteneur doit disposer
de sa propre `DataSource` JNDI `java:comp/env/jdbc/cynema`. Configurer la
DataSource dans le conteneur de l'API avec la même base et des identifiants
injectés par l'environnement ou un gestionnaire de secrets. Le POM ne crée pas
la base et l'API n'utilise pas l'EntityManager de l'application principale.
Sous Tomcat, le pilote `mysql-connector-j` doit être installé dans le dossier
`lib` du serveur ; la dépendance de l'API est donc fournie par le conteneur et
n'est pas recopiée dans le WAR. Préférer un contexte Tomcat propre à
`cynemma-api` pour la DataSource afin de ne pas diffuser ses identifiants aux
autres applications.

Variables nécessaires :

| Variable | Processus | Usage |
|---|---|---|
| `TMDB_API_KEY` | API | Accès serveur à TMDB |
| `CYNEMMA_JWT_PUBLIC_KEY` | API | Validation des jetons employés |
| `CYNEMMA_JWT_PRIVATE_KEY` | Application principale | Signature des jetons employés |
| `CYNEMMA_API_BASE_URL` | Application principale | Origine + contexte du WAR API, sans `/api` final |

Pour le développement local, les deux WAR lisent chaque valeur dans cet ordre :
variable d'environnement, propriété système Java du même nom, puis fichier
`.local-secrets/cynemma.env`. Le fichier est recherché depuis le répertoire de
lancement et les répertoires Tomcat ; il peut aussi être indiqué avec
`-Dcynemma.config.file=/chemin/absolu/cynemma.env`. Sous Tomcat, le fichier
`${catalina.base}/conf/cynemma.env` est également pris en charge.
Ne versionnez jamais ce fichier : il contient notamment la clé privée JWT.

L'absence de `TMDB_API_KEY` n'empêche pas le démarrage du WAR. L'endpoint santé
vérifie MySQL indépendamment de TMDB ; les endpoints qui nécessitent TMDB
répondent HTTP 503 avec un message JSON explicite jusqu'à la configuration de
la clé. Un texte de remplacement tel que `METTRE_TA_CLE_TMDB_ICI` est rejeté
explicitement.

### Configurer une clé TMDB en développement local

1. Obtenir une clé API depuis les paramètres API du compte TheMovieDB.
2. Ajouter la valeur au fichier ignoré par Git
   `.local-secrets/cynemma.env`, à la racine du dépôt :

   ```text
   TMDB_API_KEY=votre_cle_tmdb
   ```

3. La clé peut également être placée dans
   `${catalina.base}/conf/cynemma.env` avec la même syntaxe :

   ```text
   TMDB_API_KEY=votre_cle_tmdb
   ```

4. Redémarrer Tomcat après toute modification du fichier.
5. Vérifier le résultat avec cette requête locale :

   ```http
   GET http://localhost:8080/cynemma-api/api/trailer?film=Interstellar&year=2014
   Accept: application/json
   ```

Une valeur `METTRE_TA_CLE_TMDB_ICI` ou
`REPLACE_WITH_REAL_TMDB_API_KEY` est considérée comme un placeholder et refuse
de contacter TMDB. Une erreur `503` avec
`{"erreur":"Le service des bandes-annonces n'est pas configuré"}` indique que
la clé est absente, vide ou encore remplacée par un placeholder.

Ne placez jamais la clé dans le POM Maven, dans une URL Postman, dans les
sources Java ou dans un fichier suivi par Git. Ne publiez pas les captures
d'écran contenant sa valeur.

Pour le déploiement local proposé ci-dessous, `CYNEMMA_API_BASE_URL` vaut
`http://localhost:8080/cynemma-api` lorsque les deux WAR sont déployés sur le
même Tomcat. Si les applications ne sont pas sur le même hôte, utiliser HTTPS
et limiter l'accès réseau aux appels nécessaires.

Le schéma existant `tmdb_cache` reste partagé. Les transactions sont locales à
chaque application ; aucune transaction JPA distribuée n'est créée. Les
opérations de cache sont indépendantes de la persistance du film importé par
l'application principale.

## Construction et démarrage

Prérequis : JDK 21, Maven, un conteneur compatible Jakarta Servlet 6.1 et une
DataSource configurée.

Construire séparément l'API :

```bash
cd API
mvn clean package
```

Déployer `target/cynemma-api.war` dans le même Tomcat que l'application
principale, sous le contexte `cynemma-api`. Avant le démarrage, configurer
`TMDB_API_KEY`, `CYNEMMA_JWT_PUBLIC_KEY` et la DataSource JNDI de ce contexte.
Vérifier `http://localhost:8080/cynemma-api/api/health`.

Dans le conteneur de l'application principale, configurer
`CYNEMMA_API_BASE_URL` et `CYNEMMA_JWT_PRIVATE_KEY`, puis déployer le WAR
principal. Démarrer l'API avant d'utiliser l'import TMDB ; si l'API est
indisponible, les pages et validations locales restent disponibles, mais les
opérations de suggestion/import échouent explicitement et ne prétendent pas
avoir réussi.

La route `GET /api/trailer` est facultative pour l'affichage du catalogue ; sa
défaillance ne bloque pas la consultation des films. Elle reste accessible par
le chemin historique de l'application principale, qui relaie la requête.

## Tests

Depuis `API/`, exécuter `mvn test`. Depuis `cynemma/`, exécuter
`mvn test`. Les tests présents couvrent le mapping et les validations initiales
de `GET /api/trailer`; aucun test d'intégration n'exerce encore les deux
conteneurs, MySQL ou TMDB.

## Documentation

Ouvrir [Documentation/openapi.yaml](Documentation/openapi.yaml) dans 42Crunch
OpenAPI Editor. Le serveur inscrit dans le fichier est une URL de développement
indicative ; l'URL effective dépend du conteneur. Le schéma OpenAPI ne remplace
pas l'authentification serveur à serveur et ne contient aucun secret.
