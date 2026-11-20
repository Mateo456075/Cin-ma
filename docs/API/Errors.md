# Erreurs, limites et points à confirmer

## Endpoint JSON `GET /api/trailer`

Les erreurs JSON utilisent actuellement la propriété `erreur`.

| Statut | Situation observée | Exemple de corps |
|---:|---|---|
| 400 | `film` absent/blanc, année non numérique ou valeur rejetée par le service | `{"erreur":"Paramètre 'film' manquant"}` |
| 404 | Aucune bande-annonce disponible ou aucun résultat exploitable | `{"erreur":"Aucune bande-annonce disponible"}` |
| 500 | Erreur SQL/JNDI ou erreur interne | `{"erreur":"Erreur interne"}` |
| 502 | Erreur HTTP/réseau TMDB ou réponse JSON TMDB invalide | `{"erreur":"Le service TMDB est temporairement indisponible"}` |
| 503 | Clé TMDB absente/mal configurée lors d'un cache miss, ou appel interrompu | Ex. `{"erreur":"Le service des bandes-annonces n'est pas configuré"}` |
| 504 | Délai de réponse TMDB dépassé | `{"erreur":"Le service TMDB est temporairement indisponible"}` |

Le succès renvoie HTTP 200 et un objet comportant `key`, `watchUrl` et
`embedUrl`. Les exemples complets figurent dans la
[référence des endpoints](Endpoints.md#api-json--war-api).

Le paramètre `year` est converti en entier par la servlet. L'année hors de
l'intervalle de validation du service est rejetée comme entrée invalide. Le
titre est normalisé par le service et limité à 255 caractères.

## Routes MVC et erreurs de formulaire

Ces routes ne renvoient pas de format d'erreur JSON commun :

- les réponses d'erreur explicites avec `sendError` utilisent les codes décrits
  dans la référence des routes ;
- les erreurs de saisie sont souvent réaffichées dans une JSP, généralement
  sans changement explicite de statut ;
- les redirections utilisent `sendRedirect` ;
- certaines exceptions de conversion ou d'accès aux données ne sont pas
  explicitement traduites en réponse par la servlet et peuvent être traitées
  par le conteneur.

## Incohérences et limites constatées dans le code

1. **Les deux WAR n'exposent pas le même type de routes.** Dans l'application
   principale, `/api/trailer` est un relais JSON et les autres routes décrites
   ici servent principalement l'interface MVC. Le WAR `API/` expose aussi
   `/api/health` et les routes JSON internes `/api/internal/tmdb/*` ; le
   téléchargement d'affiche de cette API renvoie du contenu binaire.
2. **Authentification non uniforme.** Certaines routes contrôlent session et
   rôle, d'autres sont publiques. `/api/trailer` n'exige pas de session.
3. **La déconnexion utilise GET.** `/deconnexion` invalide la session par une
   requête GET.
4. **La création de compte et la connexion ne vérifient pas de jeton CSRF dans
   leurs servlets.**
5. **`/page?path=...` n'exige pas de session, mais applique une liste blanche.**
   Seules les vues `Authentification`, `Creat` et `visiteur/visiteur` sont
   transférées ; les autres valeurs, y compris les vues protégées et les
   tentatives de traversal, reçoivent HTTP 404.
6. **La modification d'un film n'a pas de page d'erreur métier uniforme si le
   DAO échoue.** La servlet lève une `ServletException` au lieu de rediriger
   vers la liste après un échec de mise à jour.
7. **La création d'une rangée et de ses sièges s'effectue via plusieurs appels
   DAO distincts.** Le code visible ne les regroupe pas dans une seule
   transaction.
8. **La création de séances vérifie les conflits d'horaire dans un DAO, mais
   l'expression SQL native et ses règles dépendent de MySQL et de sa
   compatibilité.**
9. **La réservation produit un rendu JSP et un courriel tenté côté serveur.**
   L'envoi du courriel peut échouer indépendamment de la réservation.
10. **Le parcours d'import TMDB est une interaction HTML employé.** Ses erreurs
    dépendent du stade (validation, appel externe, sauvegarde) et ne définissent
    pas de contrat JSON.

Ces remarques décrivent les comportements et limites visibles dans les sources ;
elles ne constituent pas une validation en exécution.

## Informations non déterminées à partir du code seul

- Le contexte et le nom de domaine utilisés au déploiement.
- Les paramètres du conteneur et l'association effective de
  `java:comp/env/jdbc/cynema` à une `DataSource`.
- La configuration SMTP réellement disponible.
- Les codes et pages d'erreur finaux produits par le conteneur pour les
  exceptions non capturées.
- Le comportement réel de TMDB et de MySQL en production.
