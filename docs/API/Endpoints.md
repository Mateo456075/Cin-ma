# Référence des endpoints et routes

Toutes les URLs ci-dessous sont relatives au contexte de déploiement. Par
exemple, avec un contexte `/cinema`, `/api/trailer` devient
`/cinema/api/trailer`.

## API JSON

### Obtenir une bande-annonce

**Description :** recherche une bande-annonce YouTube avec TMDB, après
consultation du cache local.

| Propriété | Valeur |
|---|---|
| Méthode et endpoint | `GET /api/trailer` |
| Catégorie | Bande-annonce |
| Authentification | Aucune vérification de session ou de rôle dans cette servlet |
| Type de réponse | `application/json; charset=UTF-8` |
| Corps de requête | Aucun |
| En-têtes obligatoires | Aucun |

#### Paramètres de requête

| Nom | Type | Obligatoire | Validation observée |
|---|---|---:|---|
| `film` | chaîne | Oui | Non null et non blanche ; le service normalise les espaces et limite le titre à 255 caractères |
| `year` | entier | Non | Si présent, doit être numérique et compris entre 1888 et 9999 |

#### Exemple

```http
GET /cinema/api/trailer?film=Interstellar&year=2014
Accept: application/json
```

#### Réponse réussie — HTTP 200

```json
{
  "key": "zSWdZVtXT7E",
  "watchUrl": "https://www.youtube.com/watch?v=zSWdZVtXT7E",
  "embedUrl": "https://www.youtube-nocookie.com/embed/zSWdZVtXT7E"
}
```

Les valeurs de clé ci-dessus illustrent le format ; elles ne sont pas garanties
comme résultat TMDB actuel pour ce titre.

#### Erreurs

| Statut | Cas |
|---:|---|
| 400 | `film` absent/blanc, `year` non numérique ou valeur invalide rejetée par le service |
| 404 | Aucune bande-annonce disponible |
| 500 | Erreur SQL/JNDI ou configuration TMDB manquante |
| 502 | Erreur réseau/HTTP TMDB ou réponse JSON TMDB invalide |
| 503 | Requête interrompue |
| 504 | Délai d'attente TMDB dépassé |

Les réponses d'erreur contiennent une propriété `erreur`, par exemple :

```json
{"erreur":"Aucune bande-annonce disponible"}
```

#### Remarques

- Le service consulte `tmdb_cache` avant TMDB. Une clé positive expire après
  90 jours ; une absence de bande-annonce expire après 7 jours.
- Si `year` est fourni, l'année est incluse dans la clé textuelle de cache.
- Le service privilégie une vidéo YouTube de type `Trailer` avec
  `official=true`, avec repli sur la première clé valide, et cherche d'abord en
  français puis en anglais.
- Aucun en-tête CORS permissif n'est défini. Cela ne remplace pas un contrôle
  d'accès côté serveur.
- Référence : [`TrailerServlet`](../../cynemma/src/main/java/fr/turgot/servlet/api/TrailerServlet.java)
  et [`TmdbService`](../../cynemma/src/main/java/fr/turgot/service/TmdbService.java).

## Routes MVC

Les routes de cette section sont des interfaces Servlet de l'application. Les
GET rendent une page JSP ou redirigent ; les POST reçoivent des paramètres de
formulaire et ne renvoient pas de JSON.

Pour les POST protégés par CSRF, le champ `csrfToken` doit contenir le jeton
fourni à la JSP. Les formulaires HTML utilisent généralement
`application/x-www-form-urlencoded`, sauf la création manuelle d'un film, qui
utilise `multipart/form-data`.

### Accueil — `GET /accueil`

- **Accès :** public.
- **Paramètres/corps :** aucun.
- **Traitement :** charge les films puis transfère vers `/index.jsp`.
- **Réponse :** page HTML ; le statut n'est pas fixé explicitement.
- **Source :** [`AccueilServlet`](../../cynemma/src/main/java/fr/turgot/servlet/AccueilServlet.java).

### Connexion — `GET, POST /connexion`

**GET**

- **Accès :** public ; aucun paramètre.
- **Réponse :** formulaire de connexion en JSP.

**POST**

- **Accès :** public ; pas de contrôle CSRF visible dans cette servlet.
- **Paramètres de formulaire :** `login`, `mdp`.
- **Validation :** les deux valeurs doivent être présentes et non blanches.
- **Succès :** ouvre une session et y stocke l'utilisateur sous `usere`.
  Le client est redirigé vers `/client`; les vues employé et administrateur
  sont transférées dans la servlet.
- **Échec :** réaffiche le formulaire avec un message, sans statut HTTP d'erreur
  explicitement défini.
- **Source :** [`AuthentificationServlet`](../../cynemma/src/main/java/fr/turgot/servlet/AuthentificationServlet.java).

### Création de compte — `GET, POST /nouveaucompte`

**GET**

- **Accès :** public ; aucun paramètre.
- **Réponse :** formulaire JSP de création de compte.

**POST**

- **Accès :** public ; pas de contrôle CSRF visible dans cette servlet.
- **Paramètres de formulaire :** `nom`, `login`, `mdp`, `confirm_mdp`, `email`.
- **Validation :** champs non vides, confirmation identique ; adresse contrôlée
  par une expression régulière dans la servlet.
- **Succès :** le DAO crée un compte de type client et hache le mot de passe
  avec BCrypt ; la servlet transfère vers la page de connexion.
- **Échec :** réaffiche le formulaire avec un message ; aucun statut d'erreur
  explicite n'est défini.
- **Source :** [`CreatServlet`](../../cynemma/src/main/java/fr/turgot/servlet/CreatServlet.java)
  et [`UserDAO`](../../cynemma/src/main/java/fr/turgot/dao/UserDAO.java).

### Changement de mot de passe — `GET, POST /changerMotDePasse`

**GET**

- **Accès :** session requise, tous rôles connectés.
- **Réponse :** formulaire avec jeton CSRF ; sans utilisateur, redirection vers
  `/index.jsp`.

**POST**

- **Accès :** session requise ; jeton CSRF obligatoire.
- **Paramètres de formulaire :** `csrfToken`, `oldPassword`, `newPassword`,
  `confirmPassword`.
- **Validation :** champs obligatoires, confirmation identique et vérification
  de l'ancien mot de passe.
- **Résultat :** mot de passe enregistré haché par BCrypt puis formulaire
  réaffiché avec un message.
- **Statut explicite :** 403 si session absente (redirection) ou jeton CSRF
  invalide (`sendError` pour le jeton).
- **Source :** [`ChangePasswordServlet`](../../cynemma/src/main/java/fr/turgot/servlet/ChangePasswordServlet.java).

### Déconnexion — `GET /deconnexion`

- **Accès :** public ; aucun paramètre ni jeton CSRF vérifié.
- **Traitement :** invalide la session si elle existe.
- **Réponse :** redirection vers `/index.jsp`.
- **Remarque :** l'action qui modifie l'état de session est exposée en GET dans
  le code actuel.
- **Source :** [`LogoutServlet`](../../cynemma/src/main/java/fr/turgot/servlet/LogoutServlet.java).

### Transfert vers une page JSP — `GET /page`

- **Accès :** aucune vérification de session ou de rôle visible.
- **Paramètre de requête :** `path`.
- **Traitement :** si `path` est non vide, construit
  `LinkHelper.JSP_ROOT + path + ".jsp"` et tente un transfert interne.
- **Réponse :** JSP ou erreur de dispatcher ; si `path` est absent/vide,
  `sendError(404)`.
- **Remarque :** aucune liste blanche de chemins n'est visible dans cette
  servlet. Les cibles effectivement accessibles dépendent du conteneur.
- **Source :** [`PageServlet`](../../cynemma/src/main/java/fr/turgot/utils/PageServlet.java).

### Liste des séances employé — `GET /employe/listeSeances`

- **Accès :** rôle `EMPLOYE` en session ; sinon redirection vers `/index.jsp`.
- **Paramètres/corps :** aucun.
- **Traitement :** charge séances et films, prépare `filmsMap`.
- **Réponse :** JSP employé.
- **Source :** [`SeanceListServlet`](../../cynemma/src/main/java/fr/turgot/servlet/SeanceListServlet.java).

### Liste des films de gestion — `GET /listeFilms`

- **Accès :** rôle `EMPLOYE` en session ; sinon redirection vers `/index.jsp`.
- **Paramètres/corps :** aucun.
- **Traitement :** charge tous les films et prépare un jeton CSRF pour les
  actions du formulaire.
- **Réponse :** JSP employé `FilmE.jsp`.
- **Source :** [`FilmListServlet`](../../cynemma/src/main/java/fr/turgot/servlet/client/FilmListServlet.java).

### Espace client — `GET /client`

- **Accès :** rôle `CLIENT` en session ; sinon redirection vers `/index.jsp`.
- **Paramètres/corps :** aucun.
- **Traitement :** charge la liste des films.
- **Réponse :** JSP client.
- **Source :** [`ClientServlet`](../../cynemma/src/main/java/fr/turgot/servlet/client/ClientServlet.java).

### Séances d'un film — `GET /client/listeSeances`

- **Accès :** rôle `CLIENT` en session ; sinon redirection vers `/index.jsp`.
- **Paramètre de requête :** `filmId` (entier attendu par `Integer.parseInt`).
- **Traitement :** charge le film et ses séances.
- **Si `filmId` absent :** redirection vers `/client`.
- **Si `filmId` n'est pas un entier :** aucune traduction explicite de
  l'exception n'est définie dans la servlet.
- **Réponse :** JSP client.
- **Source :** [`SeanceListClientServlet`](../../cynemma/src/main/java/fr/turgot/servlet/client/SeanceListClientServlet.java).

### Réservation — `GET, POST /billetterie`

**GET**

- **Accès :** rôle `CLIENT` en session ; sinon redirection vers `/index.jsp`.
- **Paramètres de requête :** `seanceId`, `filmId` (entiers attendus).
- **Traitement :** charge la séance, le film, la salle et le nombre de places
  disponibles ; fournit un jeton CSRF.
- **Réponse :** formulaire JSP de réservation.

**POST**

- **Accès :** rôle `CLIENT` en session ; `csrfToken` obligatoire.
- **Paramètres de formulaire :** `csrfToken`, `seanceId`, `filmId`, `nbPlaces`.
- **Traitement :** vérifie le nombre de places disponibles, crée une
  réservation et des billets avec des sièges attribués par le DAO, puis tente
  d'envoyer un courriel.
- **Échec de disponibilité :** réaffiche le formulaire avec un message.
- **Succès :** réaffiche la JSP avec le message de confirmation ; aucun JSON.
- **Remarque :** le code ne traduit pas explicitement toutes les erreurs de
  conversion de paramètres.
- **Source :** [`BilletServlet`](../../cynemma/src/main/java/fr/turgot/servlet/client/BilletServlet.java)
  et [`BilletDAO`](../../cynemma/src/main/java/fr/turgot/dao/BilletDAO.java).

### Création manuelle d'un film — `GET, POST /creerfilm`

**GET**

- **Accès :** rôle `EMPLOYE` en session ; réponse 403 sinon.
- **Traitement :** prépare un jeton CSRF et affiche le formulaire.

**POST**

- **Accès :** rôle `EMPLOYE` ; `csrfToken` obligatoire.
- **Type de contenu :** `multipart/form-data`.
- **Champs :** `csrfToken`, `titre`, `realisateur`, `annee`, `temps`, `genre`,
  fichier `affiche`.
- **Validation observée :** champs obligatoires, année et durée parsables en
  entier, image décodable PNG/JPEG avec dimensions positives et au plus
  20 000 000 pixels. Limites Servlet : 10 Mio par fichier et 50 Mio par requête.
- **Succès :** écrit l'affiche réencodée en PNG sous `image/`, crée le film puis
  redirige vers `/listeFilms`.
- **Erreurs explicites :** 403 pour accès/CSRF, 400 pour champs, nombres ou
  images invalides. Une erreur de persistance lève une `ServletException`.
- **Source :** [`CreatFilmServlet`](../../cynemma/src/main/java/fr/turgot/servlet/employe/CreatFilmServlet.java).

### Modification d'un film — `GET, POST /modifierFilm`

**GET**

- **Accès :** rôle `EMPLOYE` en session ; sinon redirection vers `/index.jsp`.
- **Paramètre de requête :** `id`.
- **Traitement :** charge le film et affiche le formulaire s'il existe ; sinon
  redirige vers `/listeFilms`.

**POST**

- **Accès :** rôle `EMPLOYE` ; `csrfToken` obligatoire.
- **Paramètres de formulaire :** `csrfToken`, `id`, `titre`, `realisateur`,
  `annee`, `genre`, `temps`, `ancienneAffiche`.
- **Traitement :** construit un `Film`, appelle le DAO de mise à jour, puis
  redirige vers `/listeFilms`.
- **Remarque :** les exceptions de conversion sont capturées sans réponse
  d'erreur spécifique ; la servlet redirige ensuite comme après le traitement.
- **Source :** [`EditFilmServlet`](../../cynemma/src/main/java/fr/turgot/servlet/employe/EditFilmServlet.java).

### Suppression d'un film — `POST /supprimerFilm`

- **Accès :** rôle `EMPLOYE` ; `csrfToken` obligatoire.
- **Paramètres de formulaire :** `csrfToken`, `id`.
- **Traitement :** demande au DAO de supprimer le film. Le DAO refuse la
  suppression s'il existe des séances rattachées.
- **Réponses :** 403 pour accès/CSRF ; 400 pour un identifiant non numérique ;
  redirection vers `/listeFilms?error=seance` si le DAO refuse la suppression ;
  sinon redirection vers `/listeFilms`.
- **Source :** [`DeleteFilmServlet`](../../cynemma/src/main/java/fr/turgot/servlet/employe/DeleteFilmServlet.java)
  et [`FilmDAO`](../../cynemma/src/main/java/fr/turgot/dao/FilmDAO.java).

### Création d'une séance — `GET, POST /nouvelleSeance`

**GET**

- **Accès :** rôle `EMPLOYE` ; 403 sinon.
- **Traitement :** charge films et salles, prépare un jeton CSRF et affiche le
  formulaire.

**POST**

- **Accès :** rôle `EMPLOYE` ; `csrfToken` obligatoire.
- **Paramètres de formulaire :** `csrfToken`, `filmId`, `salleId`, `date`
  (`yyyy-MM-dd`), `horaire` (`HH:mm`).
- **Traitement :** compose une date/heure, vérifie les conflits de salle (durée
  du film plus 20 minutes selon le DAO), puis crée la séance si libre.
- **Réponse :** formulaire réaffiché avec un message de succès ou d'erreur ;
  l'erreur de format numérique/date n'a pas de statut HTTP explicite.
- **Source :** [`CreatSeanceServlet`](../../cynemma/src/main/java/fr/turgot/servlet/employe/CreatSeanceServlet.java)
  et [`SeanceDAO`](../../cynemma/src/main/java/fr/turgot/dao/SeanceDAO.java).

### Import assisté TMDB — `GET, POST /employe/tmdb/catalogue`

**GET**

- **Accès :** rôle `EMPLOYE` ; `sendError(403)` sinon.
- **Paramètre facultatif :** `tmdbId`.
- **Sans `tmdbId` :** charge la première page de films « now playing » de TMDB et
  affiche les suggestions.
- **Avec `tmdbId` :** charge les détails et affiche le formulaire de revue.
- **Erreurs explicites :** 400 identifiant invalide, 503 interruption, 502
  erreur de communication TMDB, 500 autre erreur d'exécution.

**POST**

- **Accès :** rôle `EMPLOYE` ; champ `csrfToken` obligatoire.
- **Paramètres de formulaire :** `csrfToken`, `tmdbId`, `title`, `director`,
  `year`, `duration`, `genre`.
- **Validation observée :** identifiant TMDB positif ; titre et réalisateur
  requis (maximum 255 caractères), année 1895–2100, genre requis (maximum
  100 caractères), durée positive ; une affiche TMDB doit être disponible.
- **Traitement :** recharge les détails TMDB, met en cache la bande-annonce,
  télécharge/réencode l'affiche et persiste le film. Le film est soumis à
  validation par l'employé avant l'import.
- **Succès :** redirection vers `/listeFilms`.
- **Erreurs :** 403 accès/CSRF, 400 identifiant invalide ; validation de
  formulaire réaffichée dans la JSP ; 503 interruption, 502 communication
  TMDB/affiche, 500 erreur interne/SQL.
- **Remarque :** ce parcours HTML n'est pas une API JSON et ne remplace pas la
  création manuelle.
- **Source :** [`TmdbCatalogImportServlet`](../../cynemma/src/main/java/fr/turgot/servlet/employe/TmdbCatalogImportServlet.java)
  et [`TmdbReview.jsp`](../../cynemma/src/main/webapp/WEB-INF/Employer/TmdbReview.jsp).

### Tableau d'administration — `GET /admins`

- **Accès :** rôle `ADMINISTRATEUR` en session ; sinon redirection vers
  `/index.jsp`.
- **Paramètres/corps :** aucun.
- **Traitement :** charge utilisateurs et salles, prépare un jeton CSRF.
- **Réponse :** JSP d'administration.
- **Source :** [`AdminServlet`](../../cynemma/src/main/java/fr/turgot/servlet/admin/AdminServlet.java).

### Création d'une salle — `GET, POST /creersalle`

**GET**

- **Accès :** rôle `ADMINISTRATEUR` en session ; sinon redirection vers
  `/index.jsp`.
- **Traitement :** prépare les valeurs des énumérations de type et disposition,
  un jeton CSRF et le formulaire.

**POST**

- **Accès :** rôle `ADMINISTRATEUR` ; `csrfToken` obligatoire.
- **Paramètres de formulaire :** `csrfToken`, `nom`, `capacite`, `typeSalle`,
  `disposition`, `nombreBalcons`.
- **Valeurs d'énumération :** `QUATRE_DX`, `CANAPE`, `LIT`, `CONFOR_PLUS`,
  `STANDARD` pour le type ; `IMAX`, `SCREEN_X`, `RECTANGLE`, `ARC_DE_CERCLE`
  pour la disposition.
- **Traitement :** crée la configuration et la salle.
- **Réponses :** 403 accès/CSRF ; champs absents ou valeur non parsable/enum
  invalide conduisent à une redirection vers `/creersalle` ; succès redirige
  vers `/admins`.
- **Source :** [`CreatSalleServlet`](../../cynemma/src/main/java/fr/turgot/servlet/admin/CreatSalleServlet.java).

### Gestion des rangées et sièges — `GET, POST /gererSieges`

**GET**

- **Accès :** rôle `ADMINISTRATEUR` en session ; sinon redirection vers
  `/index.jsp`.
- **Paramètre de requête :** `salleId`.
- **Traitement :** charge la salle et ses rangées, prépare un jeton CSRF.
- **Réponse :** JSP de gestion ; paramètres invalides ou salle introuvable
  redirigent vers `/admins`.

**POST**

- **Accès :** rôle `ADMINISTRATEUR` ; `csrfToken` obligatoire.
- **Paramètres de formulaire :** `csrfToken`, `salleId`, `nomRangee`,
  `nombreSieges`.
- **Validation :** salle existante et nombre de sièges positif.
- **Traitement :** crée une rangée et les sièges numérotés de 1 à N, puis
  redirige vers `/gererSieges?salleId=...`.
- **Erreurs :** 403 accès/CSRF ; paramètres invalides, salle absente ou nombre
  non valide redirigent vers `/admins`.
- **Source :** [`CreatSiegeServlet`](../../cynemma/src/main/java/fr/turgot/servlet/admin/CreatSiegeServlet.java).

### Suppression d'un utilisateur — `POST /supprimerUtilisateur`

- **Accès :** rôle `ADMINISTRATEUR` ; `csrfToken` obligatoire.
- **Paramètres de formulaire :** `csrfToken`, `login`.
- **Traitement :** supprime l'utilisateur si `login` est fourni ; après
  suppression réussie, invalide ses sessions actives suivies par l'application.
- **Réponses :** 403 accès/CSRF ; redirection vers `/admins` autrement, y compris
  quand `login` est absent ou vide.
- **Source :** [`DeleteUserServlet`](../../cynemma/src/main/java/fr/turgot/servlet/admin/DeleteUserServlet.java).

### Modification d'un rôle — `GET, POST /modifierrole`

**GET**

- **Accès :** rôle `ADMINISTRATEUR` en session ; sinon redirection vers
  `/index.jsp`.
- **Paramètres de requête :** `login`, `role` (valeurs reprises pour afficher
  le formulaire).
- **Réponse :** JSP avec jeton CSRF.

**POST**

- **Accès :** rôle `ADMINISTRATEUR` ; `csrfToken` obligatoire.
- **Paramètres de formulaire :** `csrfToken`, `login`, `newRole`.
- **Rôles acceptés par le DAO :** `CLIENT`, `EMPLOYE`, `EMPLOYER`,
  `ADMINISTRATEUR`, `ADMIN` (comparaison sans distinction de casse).
- **Succès :** met à jour le rôle, invalide les sessions enregistrées pour ce
  login et redirige vers `/admins`.
- **Erreurs :** 403 accès/CSRF ; 400 si login/rôle absent ou mise à jour
  refusée.
- **Source :** [`RoleModifierServlet`](../../cynemma/src/main/java/fr/turgot/servlet/admin/RoleModifierServlet.java)
  et [`UserDAO`](../../cynemma/src/main/java/fr/turgot/dao/UserDAO.java).

## Inventaire par catégorie

| Catégorie | Routes |
|---|---|
| Public et compte | `/accueil`, `/page`, `/connexion`, `/nouveaucompte`, `/changerMotDePasse`, `/deconnexion` |
| Catalogue et programmation | `/listeFilms`, `/creerfilm`, `/modifierFilm`, `/supprimerFilm`, `/nouvelleSeance`, `/employe/listeSeances`, `/employe/tmdb/catalogue` |
| Client et réservation | `/client`, `/client/listeSeances`, `/billetterie` |
| Administration | `/admins`, `/creersalle`, `/gererSieges`, `/supprimerUtilisateur`, `/modifierrole` |
| JSON | `/api/trailer` |
