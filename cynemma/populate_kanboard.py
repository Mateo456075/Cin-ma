#!/usr/bin/env python3
"""Script de gestion et synchronisation robuste du backlog Kanboard du projet Cinema (Cynemma).

Fonctionnalités :
- Analyse et couverture complète des 4 rôles (Visiteur, Client, Employé, Administrateur).
- Organisation rigoureuse en 9 catégories techniques.
- Statuts réels vérifiés sur le code : Terminé (Done), En cours (Work in progress), À faire (TO DO / Backlog).
- Planning réaliste sans dates passées pour les tâches non terminées.
- Mode aperçu sans modification par défaut (--preview / sans --apply).
- Mode simulation hors-ligne (--snapshot <fichier.json>) sans nécessiter de connexion.
- Sauvegarde intégrale automatique du projet avant toute écriture.
- Confirmation interactive explicite requise avant toute suppression/remplacement.
- Gestion des catégories, des sous-tâches, des priorités et des liens de dépendance (« is blocked by »).
- Vérification post-création de l'intégrité des tâches créées.
- Prévention des doublons et protection des identifiants (variables d'environnement, secrets locaux, prompt).
"""

import argparse
import getpass
import json
import os
import re
import sys
import unicodedata
from datetime import date, datetime, timedelta
from typing import Any, Optional

# ==============================================================================
# CONFIGURATION ET CHARGEMENT SÉCURISÉ DES SECRETS
# ==============================================================================

DEFAULT_KANBOARD_URL = os.getenv("KANBOARD_URL", "")
DEFAULT_API_USERNAME = os.getenv("KANBOARD_API_USERNAME", "jsonrpc")
DEFAULT_API_TOKEN = os.getenv("KANBOARD_API_TOKEN", "")
DEFAULT_PROJECT_NAME = os.getenv("KANBOARD_PROJECT_NAME", "Cinema")
REQUEST_TIMEOUT_SECONDS = 30


def load_local_env_secrets() -> dict[str, str]:
    """Charge les secrets depuis .local-secrets/cynemma.env s'il existe."""
    secrets: dict[str, str] = {}
    candidate_paths = [
        os.path.join(os.path.dirname(__file__), "..", ".local-secrets", "cynemma.env"),
        os.path.join(os.getcwd(), ".local-secrets", "cynemma.env"),
        os.path.expanduser("~/.cynemma.env"),
    ]
    for path in candidate_paths:
        normalized = os.path.normpath(path)
        if os.path.isfile(normalized):
            try:
                with open(normalized, "r", encoding="utf-8") as f:
                    for line in f:
                        line = line.strip()
                        if not line or line.startswith("#") or "=" not in line:
                            continue
                        key, val = line.split("=", 1)
                        secrets[key.strip()] = val.strip().strip("\"'")
            except OSError:
                pass
            break
    return secrets


# ==============================================================================
# 9 CATÉGORIES OFFICIELLES DU PROJET
# ==============================================================================

CATEGORIES = [
    "1. Interface et navigation",
    "2. Authentification et profils",
    "3. Gestion des films et séances",
    "4. Gestion des salles et sièges 3D",
    "5. Réservation et billetterie",
    "6. Commandes et paiements",
    "7. Sécurité",
    "8. Infrastructure et tests",
    "9. Documentation et déploiement",
]

COLUMN_ALIASES = {
    "done": {"done", "termine", "terminee", "completed", "resolu"},
    "in_progress": {"workinprogress", "inprogress", "encours", "doing", "wip"},
    "todo": {"todo", "afaire", "ready", "pret", "next"},
    "backlog": {"backlog", "idees", "reservoir", "futur"},
}

COLUMN_LABELS = {
    "done": "Done",
    "in_progress": "Work in progress",
    "todo": "TO DO",
    "backlog": "Backlog",
}


# ==============================================================================
# CATALOGUE COMPLET DU BACKLOG CYNEMMA
# Vérifié sur le code source réel (Java EE Jakarta, API, JSP, SQL, Tests)
# ==============================================================================

# Date pivot pour la planification (date de référence : fin novembre 2026)
REFERENCE_DATE = date(2026, 11, 23)

BACKLOG_DATA: list[dict[str, Any]] = [
    # --------------------------------------------------------------------------
    # 1. INTERFACE ET NAVIGATION
    # --------------------------------------------------------------------------
    {
        "id_ref": 101,
        "title": "T101 - Navigation principale et design system éditorial CSS",
        "category": "1. Interface et navigation",
        "column": "done",
        "priority": 1,
        "week_offset": -3,
        "objective": "Mettre en place la charte graphique sombre, l'en-tête responsive et les composants CSS de base.",
        "description": (
            "Développement de la feuille de style globale `style.css` sans framework externe : typographies Fraunces "
            "et Atkinson Hyperlegible Next, variables CSS d'espacement et couleurs, effet de verre (glassmorphism), "
            "styles d'impression, adaptations accessibilité (prefers-reduced-motion) et header dynamique."
        ),
        "subtasks": [
            "Conception du design system dans style.css (palette, typographie, espacement)",
            "Développement du header dynamique avec affichage conditionnel par rôle (header.jsp)",
            "Intégration du footer accessible et des liens d'évitement clavier (footer.jsp)",
            "Optimisations responsive mobile/desktop et support prefers-reduced-motion",
        ],
        "acceptance_criteria": [
            "L'en-tête adapte ses liens selon l'état de session (Visiteur, Client, Employé, Admin).",
            "La navigation clavier est fonctionnelle avec indicateurs de focus visibles.",
            "Le design s'adapte sans rupture de 320px à 1920px de largeur.",
        ],
        "dependencies": [],
    },
    {
        "id_ref": 102,
        "title": "T102 - Page d'accueil et catalogue public des films à l'affiche",
        "category": "1. Interface et navigation",
        "column": "done",
        "priority": 2,
        "week_offset": -2,
        "objective": "Présenter les films actuellement à l'affiche aux visiteurs dès la page d'accueil.",
        "description": (
            "Contrôleur AccueilServlet (/accueil) interrogeant FilmDAO.getAllFilms() et routage vers index.jsp. "
            "Section Hero avec composition visuelle cinéma et catalogue en grille affichant affiche, titre, genre, "
            "durée et année avec gestion de l'état vide."
        ),
        "subtasks": [
            "Implémentation du contrôleur AccueilServlet et mapping /accueil",
            "Mise en page de la section Hero avec visuel cinéma asymétrique (index.jsp)",
            "Affichage de la grille de films via boucle JSTL <c:forEach>",
            "Gestion du fallback gracieux lorsque le catalogue est vide",
        ],
        "acceptance_criteria": [
            "L'accès à /accueil ou à la racine charge les films enregistrés en base.",
            "Chaque carte de film affiche correctement l'affiche servie par PosterImageServlet.",
            "L'état vide affiche un message explicite sans erreur 500.",
        ],
        "dependencies": [101],
    },
    {
        "id_ref": 103,
        "title": "T103 - Page publique de consultation détaillée d'un film avec bande-annonce",
        "category": "1. Interface et navigation",
        "column": "todo",
        "priority": 2,
        "week_offset": 2,
        "objective": "Offrir une fiche détaillée complète pour chaque film, accessible aux visiteurs et clients.",
        "description": (
            "Rendre les cartes de la page d'accueil cliquables vers une route dédiée `/film/detail?id=X`. "
            "Affichage de la fiche complète : synopsis, réalisateur, durée, genres, lecteur YouTube embarqué "
            "pour la bande-annonce (consommation de l'API /api/trailer) et liste des prochaines séances programmées."
        ),
        "subtasks": [
            "Créer le contrôleur FilmDetailServlet (/film/detail)",
            "Concevoir la vue JSP dédiée (fiche film, synopsis, crédits)",
            "Intégrer le lecteur vidéo YouTube via l'API cynemma-api /api/trailer",
            "Afficher la liste des séances rattachées avec bouton direct de réservation",
        ],
        "acceptance_criteria": [
            "Cliquer sur un film depuis l'accueil ouvre sa fiche détaillée.",
            "La bande-annonce YouTube s'affiche et se lit si disponible dans le cache TMDB.",
            "Les prochaines séances du film sont listées avec horaires et salle.",
        ],
        "dependencies": [102, 304],
    },
    {
        "id_ref": 104,
        "title": "T104 - Planning public de consultation des séances pour les visiteurs",
        "category": "1. Interface et navigation",
        "column": "todo",
        "priority": 2,
        "week_offset": 2,
        "objective": "Permettre aux visiteurs non authentifiés de consulter le calendrier global des séances.",
        "description": (
            "Créer une vue publique `/seances` permettant aux visiteurs de filtrer la programmation par date, film et format. "
            "Lorsqu'un visiteur clique sur « Réserver », le système mémorise la séance demandée et le redirige vers `/connexion` "
            "pour reprendre immédiatement le flux après authentification."
        ),
        "subtasks": [
            "Créer le contrôleur public SeancePublicListServlet (/seances)",
            "Concevoir la vue JSP de calendrier des séances avec filtres par jour et par film",
            "Mettre en place la redirection post-connexion avec conservation de la séance cible",
        ],
        "acceptance_criteria": [
            "Un visiteur non connecté peut parcourir les séances sans être redirigé vers l'accueil.",
            "Cliquer sur réserver redirige vers le login en conservant le paramètre de séance.",
            "Après authentification réussie, le client est redirigé directement vers la billetterie.",
        ],
        "dependencies": [102, 202, 307],
    },
    {
        "id_ref": 105,
        "title": "T105 - Espace et tableau de bord client",
        "category": "1. Interface et navigation",
        "column": "done",
        "priority": 1,
        "week_offset": -2,
        "objective": "Fournir l'interface d'accueil de l'espace client authentifié.",
        "description": (
            "Contrôleur ClientServlet (/client) vérifiant le rôle CLIENT et vue client.jsp. "
            "Affichage du message de bienvenue personnalisé, raccourcis de navigation et grille des films à l'affiche "
            "avec liens directs vers la sélection des séances."
        ),
        "subtasks": [
            "Création de ClientServlet avec restriction de rôle CLIENT",
            "Développement de client.jsp avec salutation personnalisée",
            "Liaison des cartes de films vers /client/listeSeances?filmId=X",
        ],
        "acceptance_criteria": [
            "Un utilisateur avec rôle CLIENT accède à /client après connexion.",
            "Un utilisateur non connecté ou non-client est rejeté vers l'accueil.",
            "Chaque film renvoie vers la liste des séances du film pour ce client.",
        ],
        "dependencies": [202],
    },
    {
        "id_ref": 106,
        "title": "T106 - Tableau de bord employé et navigation métier",
        "category": "1. Interface et navigation",
        "column": "in_progress",
        "priority": 2,
        "week_offset": 0,
        "objective": "Fournir à l'employé une interface centrale pour piloter films, séances et imports TMDB.",
        "description": (
            "Finalisation de l'espace employé (/WEB-INF/Employer/Employer.jsp). Correction du bug critique de routage "
            "dans FilmListServlet (qui teste actuellement le rôle \"U\" au lieu de \"EMPLOYE\") pour rétablir l'accès "
            "au catalogue des films (/listeFilms) et unifier les raccourcis vers la programmation des séances et l'import TMDB."
        ),
        "subtasks": [
            "Corriger le contrôle de rôle dans FilmListServlet.java (remplacer 'U' par 'EMPLOYE')",
            "Organiser les modules dans Employer.jsp (Gestion films, Séances, Nouveautés TMDB)",
            "Vérifier la cohérence des messages d'erreur et de succès lors des opérations employé",
        ],
        "acceptance_criteria": [
            "Un compte employé accède sans redirection anormale à /listeFilms.",
            "Le tableau de bord présente les compteurs de films à l'affiche et séances du jour.",
            "Tous les liens du dashboard pointent vers des fonctionnalités opérationnelles.",
        ],
        "dependencies": [202, 301, 307],
    },

    # --------------------------------------------------------------------------
    # 2. AUTHENTIFICATION ET PROFILS
    # --------------------------------------------------------------------------
    {
        "id_ref": 201,
        "title": "T201 - Inscription client et hachage BCrypt",
        "category": "2. Authentification et profils",
        "column": "done",
        "priority": 3,
        "week_offset": -3,
        "objective": "Permettre aux visiteurs de s'inscrire en tant que clients avec mots de passe sécurisés.",
        "description": (
            "Contrôleur CreatServlet (/nouveaucompte), vue Creat.jsp et UserDAO.create(). "
            "Contrôle de conformité des champs obligatoires, vérification de la correspondance des mots de passe, "
            "hachage cryptographique avec sel BCrypt, et persistance dans la table `utilisateurs` avec type_utilisateur CLIENT."
        ),
        "subtasks": [
            "Développer le formulaire Creat.jsp avec validation HTML5",
            "Contrôler les champs côté serveur dans CreatServlet (nom, login, email, mdp)",
            "Hacher le mot de passe via BCrypt.hashpw avec sel généré",
            "Insérer le nouvel utilisateur Client en base de données avec gestion des doublons",
        ],
        "acceptance_criteria": [
            "L'inscription crée une ligne dans `utilisateurs` avec type_utilisateur = 'CLIENT'.",
            "Le mot de passe en base commence par le préfixe BCrypt `$2a$10$`.",
            "Une tentative avec un login ou un email déjà utilisé retourne une erreur explicite.",
        ],
        "dependencies": [802],
    },
    {
        "id_ref": 202,
        "title": "T202 - Authentification multirôle et gestion de session HTTP",
        "category": "2. Authentification et profils",
        "column": "done",
        "priority": 3,
        "week_offset": -3,
        "objective": "Authentifier les utilisateurs et les aiguiller dynamiquement selon leur rôle.",
        "description": (
            "Contrôleur AuthentificationServlet (/connexion) et vue Authentification.jsp. "
            "Vérification du mot de passe avec BCrypt.checkpw(), stockage de l'objet Utilisateur en session HTTP (`usere`), "
            "enregistrement de la session dans SessionRegistry, et redirection spécifique (CLIENT -> /client, "
            "EMPLOYE -> /WEB-INF/Employer/Employer.jsp, ADMINISTRATEUR -> /admins)."
        ),
        "subtasks": [
            "Développer l'interface Authentification.jsp",
            "Implémenter UserDAO.authenticate() avec validation BCrypt",
            "Créer la session HTTP et y injecter l'attribut usere",
            "Enregistrer la session dans SessionRegistry pour traçabilité",
            "Aiguiller vers la vue ou route correspondant au rôle après login",
        ],
        "acceptance_criteria": [
            "Un couple login/mdp valide ouvre une session avec le bon type d'utilisateur.",
            "Un mot de passe erroné affiche un message d'erreur sans divulguer l'existence du compte.",
            "Les redirections post-authentification orientent chaque rôle vers son espace dédié.",
        ],
        "dependencies": [201, 701],
    },
    {
        "id_ref": 203,
        "title": "T203 - Déconnexion sécurisée et destruction de session",
        "category": "2. Authentification et profils",
        "column": "done",
        "priority": 1,
        "week_offset": -2,
        "objective": "Permettre aux utilisateurs de clore leur session de façon immédiate et définitive.",
        "description": (
            "Contrôleur LogoutServlet (/deconnexion) invalidant la session HTTP en cours (session.invalidate()) "
            "et redirigeant proprement vers la page d'accueil (/index.jsp)."
        ),
        "subtasks": [
            "Développer le contrôleur LogoutServlet (/deconnexion)",
            "Invalider la session HTTP existante et purger les attributs",
            "Rediriger vers la page d'accueil",
        ],
        "acceptance_criteria": [
            "L'appel à /deconnexion détruit la session active.",
            "Toute tentative d'accès ultérieur à une page protégée redirige vers l'accueil.",
        ],
        "dependencies": [202],
    },
    {
        "id_ref": 204,
        "title": "T204 - Modification autonome du mot de passe utilisateur",
        "category": "2. Authentification et profils",
        "column": "done",
        "priority": 2,
        "week_offset": -2,
        "objective": "Permettre à tout utilisateur connecté de renouveler son mot de passe en toute sécurité.",
        "description": (
            "Contrôleur ChangePasswordServlet (/changerMotDePasse), vue ChangePassword.jsp et UserDAO.updatePassword(). "
            "Vérification de l'authentification préalable, protection CSRF obligatoire, validation de l'ancien mot de passe "
            "via BCrypt, vérification de la confirmation du nouveau mot de passe, et mise à jour hachée en base."
        ),
        "subtasks": [
            "Concevoir le formulaire ChangePassword.jsp avec jeton CSRF",
            "Vérifier la validité de l'ancien mot de passe avec UserDAO.authenticate()",
            "Contrôler la concordance entre nouveau mot de passe et confirmation",
            "Hacher et enregistrer le nouveau mot de passe avec BCrypt",
        ],
        "acceptance_criteria": [
            "L'utilisateur doit impérativement fournir son mot de passe actuel valide.",
            "Le mot de passe mis à jour est immédiatement utilisable pour les prochaines connexions.",
            "L'opération est protégée contre les attaques CSRF.",
        ],
        "dependencies": [202, 702],
    },
    {
        "id_ref": 205,
        "title": "T205 - Administration des comptes et attribution des rôles",
        "category": "2. Authentification et profils",
        "column": "done",
        "priority": 2,
        "week_offset": -1,
        "objective": "Permettre à l'administrateur de lister les comptes, modifier leurs rôles et supprimer des utilisateurs.",
        "description": (
            "Espace admin (/admins via AdminServlet), vue admin.jsp, modification de rôle (/modifierrole via RoleModifierServlet) "
            "et suppression de compte (/supprimerUtilisateur via DeleteUserServlet). "
            "Protection CSRF sur les POST, mise à jour du rôle (CLIENT, EMPLOYE, ADMINISTRATEUR) et invalidation automatique "
            "des sessions actives de l'utilisateur ciblé via SessionRegistry.invalidateSessions()."
        ),
        "subtasks": [
            "Afficher la liste des utilisateurs inscrits dans admin.jsp",
            "Implémenter RoleModifierServlet pour basculer les rôles CLIENT/EMPLOYE/ADMINISTRATEUR",
            "Implémenter DeleteUserServlet pour supprimer un compte utilisateur",
            "Invalider immédiatement les sessions actives du compte modifié ou supprimé via SessionRegistry",
        ],
        "acceptance_criteria": [
            "Seul un utilisateur avec rôle ADMINISTRATEUR peut accéder à ces servlets.",
            "La modification de rôle invalide instantanément la session en cours de l'utilisateur concerné.",
            "La suppression supprime le compte en base et déconnecte l'utilisateur.",
        ],
        "dependencies": [202, 702],
    },
    {
        "id_ref": 206,
        "title": "T206 - Création et édition directe de comptes par l'administrateur",
        "category": "2. Authentification et profils",
        "column": "todo",
        "priority": 1,
        "week_offset": 4,
        "objective": "Permettre à l'administrateur de créer directement des comptes avec rôle défini et de modifier les profils.",
        "description": (
            "Enrichir le panneau d'administration pour permettre la création directe de comptes (employé ou administrateur) "
            "sans passer par l'inscription publique, ainsi que l'édition des informations d'un utilisateur existant (nom, login, email)."
        ),
        "subtasks": [
            "Créer AdminCreateUserServlet et le formulaire dédié avec sélection de rôle initial",
            "Créer AdminEditUserServlet pour corriger nom, email et login d'un compte",
            "Ajouter les contrôles d'unicité et validation de format d'email côté serveur",
        ],
        "acceptance_criteria": [
            "L'admin peut créer directement un compte avec le rôle EMPLOYE ou ADMINISTRATEUR.",
            "L'admin peut mettre à jour l'email ou le nom d'un compte existant.",
        ],
        "dependencies": [205],
    },

    # --------------------------------------------------------------------------
    # 3. GESTION DES FILMS ET SÉANCES
    # --------------------------------------------------------------------------
    {
        "id_ref": 301,
        "title": "T301 - Création manuelle d'un film avec téléversement d'affiche",
        "category": "3. Gestion des films et séances",
        "column": "done",
        "priority": 2,
        "week_offset": -2,
        "objective": "Permettre aux employés d'ajouter un nouveau film manuellement avec son affiche.",
        "description": (
            "Contrôleur CreatFilmServlet (/creerfilm), vue CrerFilm.jsp et FilmDAO.create(). "
            "Traitement multipart/form-data, validation serveur (titre, réalisateur, année, genre, durée positive), "
            "téléversement et ré-encodage sécurisé de l'affiche via PosterStorage.save(), enregistrement en base "
            "et suppression de l'affiche orpheline en cas d'échec SQL."
        ),
        "subtasks": [
            "Concevoir le formulaire CrerFilm.jsp avec champ d'upload multipart",
            "Valider l'ensemble des métadonnées du film côté serveur",
            "Traiter et stocker l'image via PosterStorage (vérification MIME et conversion PNG)",
            "Persister l'entité Film dans la table films via FilmDAO",
        ],
        "acceptance_criteria": [
            "Un employé authentifié peut créer un film avec affiche.",
            "Les images invalides ou malveillantes sont rejetées (HTTP 400).",
            "L'affiche est persistée hors du WAR et servie via PosterImageServlet.",
        ],
        "dependencies": [106, 703],
    },
    {
        "id_ref": 302,
        "title": "T302 - Modification des informations d'un film",
        "category": "3. Gestion des films et séances",
        "column": "done",
        "priority": 2,
        "week_offset": -2,
        "objective": "Permettre aux employés de modifier les données d'un film existant.",
        "description": (
            "Contrôleur EditFilmServlet (/modifierFilm), vue EditFilm.jsp et FilmDAO.update(). "
            "Formulaire prérempli avec les données actuelles du film, validation des modifications (titre, réalisateur, "
            "année, genre, durée) et conservation de l'affiche existante ou remplacement."
        ),
        "subtasks": [
            "Concevoir le formulaire EditFilm.jsp prérempli",
            "Implémenter la servlet EditFilmServlet (GET pour afficher, POST pour modifier)",
            "Mettre à jour l'entité Film en base via FilmDAO.update()",
            "Garantir la conservation du tmdb_id lors de la mise à jour",
        ],
        "acceptance_criteria": [
            "L'employé peut modifier les informations textuelles et la durée d'un film.",
            "Le film modifié est immédiatement visible dans le catalogue.",
            "Le lien vers TMDB (tmdb_id) n'est pas perdu lors de la modification.",
        ],
        "dependencies": [301],
    },
    {
        "id_ref": 303,
        "title": "T303 - Suppression d'un film et nettoyage sécurisé de l'affiche",
        "category": "3. Gestion des films et séances",
        "column": "done",
        "priority": 2,
        "week_offset": -2,
        "objective": "Permettre aux employés de retirer un film du catalogue et nettoyer les fichiers orphelins.",
        "description": (
            "Contrôleur DeleteFilmServlet (/supprimerFilm) et FilmDAO.delete(). "
            "Contrôle d'absence de séances rattachées (intégrité référentielle, redirection avec erreur sinon), "
            "suppression de la ligne en base, vérification qu'aucun autre film n'utilise la même affiche, "
            "et suppression sécurisée du fichier disque avec DeleteFilmServlet.isSafePosterPath() contre le path traversal."
        ),
        "subtasks": [
            "Implémenter DeleteFilmServlet en méthode POST avec jeton CSRF",
            "Vérifier les contraintes de clés étrangères (séances existantes) avant suppression",
            "Supprimer l'affiche sur disque uniquement si elle n'est plus référencée",
            "Sécuriser le chemin de suppression contre toute tentative de traversal",
        ],
        "acceptance_criteria": [
            "La suppression d'un film lié à des séances est bloquée avec message explicite.",
            "La suppression d'un film libre supprime l'enregistrement et son affiche disque.",
            "L'opération est protégée contre le path traversal et les attaques CSRF.",
        ],
        "dependencies": [301, 702],
    },
    {
        "id_ref": 304,
        "title": "T304 - Intégration TMDB : Import des nouveautés avec prévisualisation",
        "category": "3. Gestion des films et séances",
        "column": "done",
        "priority": 3,
        "week_offset": -1,
        "objective": "Permettre l'import automatisé des films à l'affiche depuis TMDB vers le catalogue local.",
        "description": (
            "Servlet TmdbCatalogImportServlet (/employe/tmdb/catalogue), vues TmdbSuggestions.jsp et TmdbReview.jsp. "
            "Consommation de cynemma-api via le client JAR dédié de l'API (/api/internal/tmdb/now-playing et /movies/{id}), "
            "formulaire de revue et ajustement des métadonnées, téléchargement de l'affiche haute définition via le client JAR, "
            "persistance en base avec tmdb_id unique, mise en cache locale de la bande-annonce YouTube (/trailer-cache) "
            "et nettoyage de l'affiche en cas d'échec SQL."
        ),
        "subtasks": [
            "Intégrer le client JAR de l'API dans les dépendances du projet principal cynemma",
            "Afficher la liste des films du moment TMDB dans TmdbSuggestions.jsp",
            "Concevoir l'écran de revue TmdbReview.jsp pour ajuster durée, genre, etc.",
            "Télécharger et persister l'affiche TMDB via le client JAR de l'API",
            "Enregistrer le film en base et déclencher la mise en cache de sa bande-annonce",
            "Empêcher les doublons grâce à la contrainte UNIQUE sur tmdb_id",
        ],
        "acceptance_criteria": [
            "Les films TMDB s'affichent avec visuel et résumé dans les suggestions.",
            "L'employé peut valider ou corriger les données avant confirmation.",
            "L'affiche est téléchargée localement et le film apparaît immédiatement au catalogue.",
            "Un film déjà importé est détecté sans créer de doublon.",
        ],
        "dependencies": [301, 701],
    },
    {
        "id_ref": 305,
        "title": "T305 - Recherche TMDB avancée par titre et date de sortie",
        "category": "3. Gestion des films et séances",
        "column": "in_progress",
        "priority": 2,
        "week_offset": 0,
        "objective": "Permettre aux employés de chercher n'importe quel film dans le catalogue mondial TMDB.",
        "description": (
            "Exposer une route de recherche `/api/internal/tmdb/search?query=...&year=...` dans le microservice API. "
            "Brancher l'appel de recherche disponible dans le client JAR de l'API et intégrer la barre de recherche textuelle "
            "avec champ d'année facultatif dans l'interface employé (TmdbSuggestions.jsp) pour rechercher un film spécifique."
        ),
        "subtasks": [
            "Ajouter la route de recherche dans InternalTmdbServlet et TmdbService",
            "Mettre à jour et valider la méthode de recherche dans le client JAR de l'API",
            "Intégrer le formulaire de recherche par mot-clé et année dans TmdbSuggestions.jsp",
            "Afficher les résultats de recherche avec bouton direct « Préparer l'ajout »",
        ],
        "acceptance_criteria": [
            "L'employé peut rechercher un film par son titre exact ou partiel.",
            "Le filtre d'année permet de discriminer les remakes ou homonymes.",
            "Les résultats retournés peuvent être importés selon le même flux de revue.",
        ],
        "dependencies": [304],
    },
    {
        "id_ref": 306,
        "title": "T306 - Gestion de la pagination des résultats TMDB",
        "category": "3. Gestion des films et séances",
        "column": "todo",
        "priority": 1,
        "week_offset": 1,
        "objective": "Gérer la navigation multi-pages sur les résultats de l'API TMDB.",
        "description": (
            "Ajouter le support du paramètre `page` dans les routes `/now-playing` et `/search` de cynemma-api. "
            "Relayer la pagination dans le client JAR de l'API et implémenter la barre de navigation paginée "
            "(Précédent, Suivant, numéro de page courante et total) dans TmdbSuggestions.jsp."
        ),
        "subtasks": [
            "Adapter TmdbService pour accepter et transmettre le paramètre page à l'API TMDB",
            "Mettre à jour le client JAR de l'API pour encapsuler les résultats paginés",
            "Ajouter les contrôles de navigation paginée dans TmdbSuggestions.jsp",
        ],
        "acceptance_criteria": [
            "L'employé peut parcourir les pages au-delà des 20 premiers résultats TMDB.",
            "Les boutons précédent/suivant sont désactivés aux bornes de la pagination.",
        ],
        "dependencies": [305],
    },
    {
        "id_ref": 307,
        "title": "T307 - Programmation et création d'une séance avec anti-chevauchement",
        "category": "3. Gestion des films et séances",
        "column": "done",
        "priority": 3,
        "week_offset": -1,
        "objective": "Permettre aux employés de planifier des séances tout en empêchant les conflits de salle.",
        "description": (
            "Contrôleur CreatSeanceServlet (/nouvelleSeance), vue CrerSeance.jsp et SeanceDAO.create(). "
            "Règle métier d'anti-chevauchement dans SeanceDAO.isSalleOccupee() : vérification SQL native qu'aucune séance existante "
            "n'occupe la même salle sur l'intervalle [début, début + durée film + 20 min de nettoyage], avec affichage d'un message d'erreur bloquant."
        ),
        "subtasks": [
            "Concevoir le formulaire CrerSeance.jsp avec listes déroulantes de films et salles",
            "Implémenter l'algorithme SQL de détection de conflit d'occupation dans SeanceDAO.isSalleOccupee",
            "Bloquer la création et afficher un message explicite en cas de chevauchement de créneau",
            "Persister la nouvelle séance en base de données",
        ],
        "acceptance_criteria": [
            "Une séance est créée si le créneau est libre.",
            "Toute séance programmée moins de 20 minutes après la fin d'un film précédent dans la même salle est refusée.",
            "La séance créée apparaît dans le planning employé et le catalogue client.",
        ],
        "dependencies": [301, 401],
    },
    {
        "id_ref": 308,
        "title": "T308 - Modification et suppression des séances programmées",
        "category": "3. Gestion des films et séances",
        "column": "todo",
        "priority": 2,
        "week_offset": 1,
        "objective": "Permettre aux employés d'ajuster ou annuler une séance avec vérification des réservations.",
        "description": (
            "Créer les méthodes update() et delete() dans SeanceDAO. Développer EditSeanceServlet (/employe/modifierSeance) "
            "et DeleteSeanceServlet (/employe/supprimerSeance). Vérifier que l'ajustement d'horaire ne crée aucun chevauchement "
            "(en excluant la séance en cours d'édition) et interdire ou encadrer la suppression si des billets ont déjà été réservés."
        ),
        "subtasks": [
            "Ajouter les méthodes update() et delete() dans SeanceDAO",
            "Créer EditSeanceServlet et la vue de modification avec vérification anti-chevauchement",
            "Créer DeleteSeanceServlet avec contrôle préalable des billets déjà vendus",
            "Ajouter les boutons d'action Modifier et Supprimer dans le tableau SeanceE.jsp",
        ],
        "acceptance_criteria": [
            "L'employé peut modifier l'horaire ou la salle d'une séance libre.",
            "Une tentative de modification sur un créneau en conflit est refusée avec message d'erreur.",
            "La suppression d'une séance comportant des billets vendus est bloquée ou notifie les réservataires.",
        ],
        "dependencies": [307],
    },

    # --------------------------------------------------------------------------
    # 4. GESTION DES SALLES ET SIÈGES 3D
    # --------------------------------------------------------------------------
    {
        "id_ref": 401,
        "title": "T401 - Création d'une salle avec configuration géométrique et balcons",
        "category": "4. Gestion des salles et sièges 3D",
        "column": "done",
        "priority": 2,
        "week_offset": -2,
        "objective": "Permettre à l'administrateur de définir les caractéristiques physiques d'une salle de projection.",
        "description": (
            "Contrôleur CreatSalleServlet (/creersalle), vue AjouterSalle.jsp et SalleDAO.createAvecConfiguration(). "
            "Choix du type de salle (QUATRE_DX, CANAPE, LIT, CONFOR_PLUS, STANDARD) et de la disposition (RECTANGLE, "
            "ARC_DE_CERCLE, SCREEN_X, IMAX) avec nombre de balcons, persistance transactionnelle liée dans `salles` et `configuration_salles`."
        ),
        "subtasks": [
            "Concevoir AjouterSalle.jsp avec sélection des énumérations TypeSalle et DispositionSieges",
            "Valider la cohérence des balcons (aucun balcon autorisé en IMAX)",
            "Persister de manière atomique la salle et sa configuration associée via JPA",
        ],
        "acceptance_criteria": [
            "L'administrateur peut enregistrer une salle avec son nom, capacité et configuration.",
            "La contrainte IMAX (0 balcon) est validée côté serveur.",
            "La nouvelle salle apparaît dans la liste d'administration /admins.",
        ],
        "dependencies": [205],
    },
    {
        "id_ref": 402,
        "title": "T402 - Modification et suppression des salles de cinéma",
        "category": "4. Gestion des salles et sièges 3D",
        "column": "todo",
        "priority": 2,
        "week_offset": 2,
        "objective": "Compléter le cycle de vie des salles pour permettre leur mise à jour et leur retrait.",
        "description": (
            "Remplacer le libellé « À venir » de la colonne action dans admin.jsp par de vraies actions Modifier et Supprimer. "
            "Ajouter les méthodes update() et delete() dans SalleDAO, créer EditSalleServlet et DeleteSalleServlet avec "
            "vérification stricte de l'absence de séances planifiées avant toute suppression de salle."
        ),
        "subtasks": [
            "Ajouter les méthodes update() et delete() dans SalleDAO",
            "Créer EditSalleServlet pour ajuster le nom, la capacité et la configuration de la salle",
            "Créer DeleteSalleServlet avec blocage si des séances sont associées à la salle",
            "Activer les liens d'action dans le tableau des salles de admin.jsp",
        ],
        "acceptance_criteria": [
            "L'admin peut modifier le nom ou la configuration d'une salle existante.",
            "La suppression d'une salle contenant des séances programmées est rejetée proprement.",
            "Une salle libre peut être supprimée avec cascade sur ses rangées et sièges.",
        ],
        "dependencies": [401],
    },
    {
        "id_ref": 403,
        "title": "T403 - Génération et calcul des coordonnées 3D des rangées et sièges",
        "category": "4. Gestion des salles et sièges 3D",
        "column": "in_progress",
        "priority": 2,
        "week_offset": 0,
        "objective": "Calculer automatiquement les coordonnées spatiales (X, Y, Z, angle) de chaque siège selon la disposition.",
        "description": (
            "Développement de SeatLayoutCalculator.applyLayout() et CreatSiegeServlet (/gererSieges). "
            "Calcul trigonométrique des positions selon la disposition : arc de cercle (rayon, courbure et orientation vers l'écran), "
            "ScreenX (courbure latérale vers les écrans secondaires), Rectangle standard, espacement des allées centrales, "
            "et élévation Y des balcons. Enregistrement des coordonnées en base dans la table `sieges`."
        ),
        "subtasks": [
            "Implémenter les formules de calcul spatial dans SeatLayoutCalculator (arc, rectangle, screenX)",
            "Calculer l'élévation Y pour les rangées en balcon (2.5m par niveau)",
            "Générer automatiquement les sièges numérotés de 1 à N lors de l'ajout d'une rangée",
            "Persister les coordonnées (coordonnee_x, coordonnee_z, angle_rotation) via SiegeDAO",
        ],
        "acceptance_criteria": [
            "Chaque siège créé possède des coordonnées X, Z et un angle de rotation cohérents.",
            "Les balcons reçoivent une altitude Y positive conforme au niveau.",
            "L'algorithme gère la présence d'une allée centrale pour les rangées de plus de 8 sièges.",
        ],
        "dependencies": [401],
    },
    {
        "id_ref": 404,
        "title": "T404 - Raccordement de l'interface d'administration des sièges",
        "category": "4. Gestion des salles et sièges 3D",
        "column": "todo",
        "priority": 1,
        "week_offset": 1,
        "objective": "Rendre accessible la gestion des sièges depuis le tableau de bord administrateur.",
        "description": (
            "Actuellement, CreatSiegeServlet (/gererSieges) et AjouterSiege.jsp existent mais ne sont pas reliés depuis admin.jsp. "
            "Remplacer le texte passif par un bouton « Configurer les sièges » pointant vers `/gererSieges?salleId=X`, "
            "permettre la génération en un clic de l'ensemble des rangées de la salle selon sa capacité, "
            "et afficher le plan 2D/3D schématique récapitulatif des sièges enregistrés."
        ),
        "subtasks": [
            "Ajouter le lien /gererSieges?salleId=X dans la colonne Action de admin.jsp",
            "Ajouter un assistant de génération globale de salle (génération automatique de toutes les rangées)",
            "Afficher un récapitulatif visuel du plan de salle configuré dans AjouterSiege.jsp",
        ],
        "acceptance_criteria": [
            "L'administrateur peut cliquer sur n'importe quelle salle pour configurer ses rangées et sièges.",
            "Les rangées existantes s'affichent avec le décompte de leurs sièges.",
        ],
        "dependencies": [402, 403],
    },
    {
        "id_ref": 405,
        "title": "T405 - Moteur de rendu 3D Three.js pour la visualisation de la salle client",
        "category": "4. Gestion des salles et sièges 3D",
        "column": "todo",
        "priority": 3,
        "week_offset": 3,
        "objective": "Offrir aux clients une vue 3D interactive et immersive de la salle de cinéma.",
        "description": (
            "Créer un endpoint JSON `/api/seances/{id}/plan-salle` exposant la géométrie de la salle et l'état des sièges. "
            "Développer le composant front-end Three.js : scène 3D avec écran courbe, estrades de balcons, modèles de sièges 3D "
            "positionnés selon les coordonnées réelles, éclairage d'ambiance salle de cinéma et caméra orbitale interactive."
        ),
        "subtasks": [
            "Créer le contrôleur API renvoyant la structure 3D de la salle et des sièges en JSON",
            "Initialiser la scène Three.js avec WebGLRenderer, caméra et lumières douces cinéma",
            "Modéliser et instancier les sièges 3D aux coordonnées (X, Y, Z, angle) fournies par l'API",
            "Implémenter la vue subjective (point de vue spectateur depuis le siège sélectionné)",
        ],
        "acceptance_criteria": [
            "Le canvas Three.js se charge de façon fluide sur la page de réservation client.",
            "La disposition spatiale reflète exactement la configuration réelle (arc, screenX, balcons).",
            "La performance reste stable (60 FPS sur navigateur standard).",
        ],
        "dependencies": [403, 501],
    },
    {
        "id_ref": 406,
        "title": "T406 - Interaction 3D et sélection interactive des sièges",
        "category": "4. Gestion des salles et sièges 3D",
        "column": "todo",
        "priority": 3,
        "week_offset": 3,
        "objective": "Permettre au client de cliquer sur un siège pour le sélectionner ou le modifier en 3D.",
        "description": (
            "Mettre en place le raycasting Three.js sur le canvas 3D : détection du survol (hover) avec mise en valeur, "
            "clic pour sélectionner/désélectionner un siège libre, code couleur temps réel (Vert = disponible, "
            "Gris = occupé, Ambre = sélectionné, Rouge = réservé temporairement), et fallback automatique vers un plan 2D SVG "
            "accessible pour les terminaux ne supportant pas WebGL."
        ),
        "subtasks": [
            "Implémenter le raycasting (Raycaster Three.js) pour détecter les interactions souris/tactile",
            "Gérer les états visuels des sièges (matériaux et shaders de couleur selon disponibilité)",
            "Synchroniser la sélection avec la liste des places du formulaire de réservation",
            "Développer le plan 2D accessible alternatif en SVG pour les critères RGAA/accessibilité",
        ],
        "acceptance_criteria": [
            "Le client peut choisir précisément ses sièges sur la vue 3D.",
            "Un siège déjà réservé est non sélectionnable.",
            "Le fallback 2D permet de réserver sans WebGL.",
        ],
        "dependencies": [405],
    },

    # --------------------------------------------------------------------------
    # 5. RÉSERVATION ET BILLETTERIE
    # --------------------------------------------------------------------------
    {
        "id_ref": 501,
        "title": "T501 - Réservation basique de places côté serveur et notification email",
        "category": "5. Réservation et billetterie",
        "column": "done",
        "priority": 3,
        "week_offset": -1,
        "objective": "Permettre à un client de réserver des places sur une séance et recevoir une confirmation email.",
        "description": (
            "Contrôleur BilletServlet (/billetterie), vue billet.jsp, et BilletDAO.createReservation(). "
            "Vérification de session client et jeton CSRF, validation des entiers positifs (seanceId, filmId, nbPlaces), "
            "attribution automatique des premiers sièges disponibles, enregistrement atomique de la réservation et des billets "
            "avec verrou pessimiste, et tentative d'envoi d'email de confirmation via Jakarta Mail / SMTP."
        ),
        "subtasks": [
            "Implémenter BilletServlet (GET pour formulaire, POST pour enregistrement)",
            "Vérifier le créneau dans le futur et le nombre de places disponibles",
            "Créer la réservation et les billets associés de manière atomique",
            "Émettre un courriel de confirmation de réservation via Jakarta Mail",
        ],
        "acceptance_criteria": [
            "Le client peut réserver un nombre N de places sur une séance ouverte.",
            "Le nombre de places disponibles diminue d'autant après la réservation.",
            "L'email de confirmation est émis si les paramètres SMTP sont configurés.",
        ],
        "dependencies": [105, 307, 705],
    },
    {
        "id_ref": 502,
        "title": "T502 - Grille tarifaire et choix du type de billet",
        "category": "5. Réservation et billetterie",
        "column": "todo",
        "priority": 2,
        "week_offset": 2,
        "objective": "Permettre au client de sélectionner ses types de billets (Plein tarif, Réduit, Enfant, VIP).",
        "description": (
            "Remplacer le champ unique nbPlaces de billet.jsp par un sélecteur par catégorie de tarif. "
            "Créer le modèle TarifBillet (Standard: 11.50€, Étudiant/Réduit: 8.90€, Moins de 14 ans: 5.50€, Supplément 3D: +2.00€). "
            "Calculer le montant total côté serveur dans BilletServlet et persister le tarif_applique exact dans chaque billet."
        ),
        "subtasks": [
            "Définir la grille tarifaire en base ou énumération avec suppléments éventuels (3D, 4DX)",
            "Modifier billet.jsp pour permettre de saisir le nombre de places par catégorie de tarif",
            "Calculer et vérifier le montant total côté serveur dans BilletServlet",
            "Persister le montant appliqué sur chaque enregistrement de la table billets",
        ],
        "acceptance_criteria": [
            "Le client peut mixer les types de billets lors d'une même commande.",
            "Le montant total de la réservation correspond fidèlement à la somme des tarifs choisis.",
            "La table billets conserve le tarif historique appliqué à chaque place.",
        ],
        "dependencies": [501],
    },
    {
        "id_ref": 503,
        "title": "T503 - API de recommandation algorithmique de sièges",
        "category": "5. Réservation et billetterie",
        "column": "todo",
        "priority": 2,
        "week_offset": 2,
        "objective": "Proposer automatiquement les meilleurs sièges contigus au centre de la salle via l'API.",
        "description": (
            "Développer un algorithme de placement dans cynemma-api (`/api/seances/{id}/recommandation-sieges?nbPlaces=N`). "
            "Critères : recherche de N sièges contigus sur une même rangée, centrage optimal par rapport à l'axe de l'écran, "
            "distance idéale (rangées centrales, rangées du fond en IMAX). Consommation par le client via le client JAR de l'API."
        ),
        "subtasks": [
            "Implémenter le service d'évaluation géométrique de qualité des sièges dans cynemma-api",
            "Exposer l'endpoint REST de recommandation protégé par jeton JWT inter-WAR",
            "Intégrer l'appel de recommandation de sièges dans le client JAR de l'API",
            "Pré-sélectionner automatiquement la proposition de sièges sur le plan interactif",
            "Permettre au client d'accepter en un clic ou de modifier manuellement la sélection",
        ],
        "acceptance_criteria": [
            "L'appel API retourne les N sièges côte à côte les mieux placés de la salle.",
            "Si aucun groupe contigu n'existe, l'algorithme propose les places les plus rapprochées.",
            "Le client voit la proposition mise en valeur et peut la valider immédiatement.",
        ],
        "dependencies": [403, 701],
    },
    {
        "id_ref": 504,
        "title": "T504 - Verrouillage temporaire des sièges (Hold / Timeout 10 min)",
        "category": "5. Réservation et billetterie",
        "column": "todo",
        "priority": 3,
        "week_offset": 2,
        "objective": "Poser un verrou temporaire sur les sièges sélectionnés pendant que le client finalise sa commande.",
        "description": (
            "Mettre en place un mécanisme de blocage temporaire (SeatHoldService) pour 10 minutes. "
            "Créer la table ou structure mémoire `sieges_reserves_temporaires` (seance_id, siege_id, session_id, date_expiration). "
            "Empêcher tout autre internaute de sélectionner un siège sous verrou actif et libérer automatiquement les sièges "
            "si le paiement n'est pas validé avant l'expiration du décompte."
        ),
        "subtasks": [
            "Créer la structure de persistance des verrous temporaires avec date d'expiration",
            "Exposer les endpoints de capture (/api/reservations/hold) et de libération (/release)",
            "Mettre en place un timer visuel (compte à rebours 10:00) sur l'écran du client",
            "Développer la tâche de fond de purge automatique des verrous expirés",
        ],
        "acceptance_criteria": [
            "Un siège verrouillé par un client devient indisponible pour tous les autres clients en temps réel.",
            "À l'issue des 10 minutes sans paiement, le verrou saute et le siège redevient libre.",
            "La finalisation du paiement transforme le verrou temporaire en billet définitif.",
        ],
        "dependencies": [501, 705],
    },
    {
        "id_ref": 505,
        "title": "T505 - Consultation de l'historique des réservations client",
        "category": "5. Réservation et billetterie",
        "column": "todo",
        "priority": 2,
        "week_offset": 5,
        "objective": "Permettre aux clients de retrouver leurs réservations passées et à venir.",
        "description": (
            "Créer le contrôleur ReservationHistoryServlet (/client/historique), la méthode JPA `ReservationDAO.findByClientId()` "
            "et la vue historique.jsp. Affichage chronologique des commandes avec détail du film, horaire de séance, salle, "
            "numéros de sièges, montant payé et statut (Confirmée, Passée)."
        ),
        "subtasks": [
            "Créer la requête JPA d'extraction des réservations avec jointures (Séances, Films, Salles, Billets)",
            "Développer le contrôleur ReservationHistoryServlet (/client/historique)",
            "Concevoir la vue JSP d'historique avec distinction entre séances à venir et passées",
            "Ajouter un lien de téléchargement direct du billet pour chaque séance à venir",
        ],
        "acceptance_criteria": [
            "Le client authentifié visualise l'ensemble de ses réservations avec leurs détails.",
            "Un client ne peut en aucun cas accéder à l'historique d'un autre client.",
        ],
        "dependencies": [501, 604],
    },
    {
        "id_ref": 506,
        "title": "T506 - Génération et téléchargement des billets électroniques en PDF",
        "category": "5. Réservation et billetterie",
        "column": "todo",
        "priority": 2,
        "week_offset": 5,
        "objective": "Générer un billet électronique PDF sécurisé avec QR code de contrôle téléchargeable par le client.",
        "description": (
            "Intégrer OpenPDF ou iText pour générer dynamiquement un document PDF de billet. "
            "Créer TicketPdfServlet (/client/telechargerBillet?reservationId=X) : contrôle de propriété de la réservation, "
            "génération du PDF (charte Cynemma, affiche du film, salle, rangée, siège, QR Code unique signé cryptographiquement "
            "pour le contrôle d'accès en entrée de salle) et envoi du fichier au navigateur avec en-tête Content-Disposition attachment."
        ),
        "subtasks": [
            "Intégrer la dépendance de génération PDF dans pom.xml",
            "Concevoir le gabarit PDF du billet (titre, affiche miniature, séance, salle, rangée, siège, tarif)",
            "Générer le QR Code contenant le jeton unique de validation de la place",
            "Implémenter la servlet sécurisée de téléchargement avec vérification du propriétaire",
        ],
        "acceptance_criteria": [
            "Le client peut télécharger son billet au format PDF depuis la confirmation ou son historique.",
            "Le PDF est propre, imprimable et contient un QR Code scannable valide.",
            "Toute tentative de téléchargement d'un billet tiers est rejetée (HTTP 403).",
        ],
        "dependencies": [505],
    },

    # --------------------------------------------------------------------------
    # 6. COMMANDES ET PAIEMENTS
    # --------------------------------------------------------------------------
    {
        "id_ref": 601,
        "title": "T601 - Catalogue et gestion des boissons et gourmandises (Snacks)",
        "category": "6. Commandes et paiements",
        "column": "todo",
        "priority": 1,
        "week_offset": 4,
        "objective": "Permettre aux clients d'ajouter des confiseries et boissons à leur séance de cinéma.",
        "description": (
            "Créer les tables `produits` et `categories_produits` en base de données. Développer les entités JPA "
            "et le DAO ProduitDAO. Intégrer un catalogue visuel des snacks (Popcorn, Confiseries, Boissons fraîches) "
            "dans le tunnel de réservation avec visuels, descriptions, tailles/formats et tarifs."
        ),
        "subtasks": [
            "Créer les tables SQL produits et catégories dans init_database.sql",
            "Créer les entités JPA Produit, CategorieProduit et le DAO ProduitDAO",
            "Développer le composant d'affichage des snacks dans le parcours de réservation",
            "Permettre la sélection des quantités par produit avec mise à jour du sous-total",
        ],
        "acceptance_criteria": [
            "Le catalogue de confiserie s'affiche lors de l'étape suivant le choix des sièges.",
            "Le client peut sélectionner des boissons ou du popcorn avec les formats souhaités.",
        ],
        "dependencies": [502],
    },
    {
        "id_ref": 602,
        "title": "T602 - Gestion du panier d'achat multi-articles en session",
        "category": "6. Commandes et paiements",
        "column": "todo",
        "priority": 2,
        "week_offset": 4,
        "objective": "Fournir un panier d'achat complet agrégeant billets de cinéma et confiseries.",
        "description": (
            "Implémenter l'objet session `Panier` agrégeant les billets réservés temporairement et les articles confiserie. "
            "Créer PanierServlet (/panier) permettant d'afficher le panier récapitulatif, de modifier les quantités de snacks, "
            "de calculer le montant total HT/TVA/TTC et d'appliquer d'éventuels avantages fidélité (table `cartes_fidelite`)."
        ),
        "subtasks": [
            "Concevoir la classe métier Panier et ses lignes d'articles en session HTTP",
            "Développer la page récapitulative du panier (/panier) avec décompte du temps restant",
            "Permettre la suppression d'un article ou modification des quantités de snacks",
            "Calculer le montant total avec taxes et intégration des points de fidélité",
        ],
        "acceptance_criteria": [
            "Le panier regroupe correctement les billets de cinéma et les produits alimentaires.",
            "Le total est recalculé dynamiquement lors de toute modification.",
            "L'expiration du timer de verrouillage des sièges vide ou adapte le panier automatiquement.",
        ],
        "dependencies": [504, 601],
    },
    {
        "id_ref": 603,
        "title": "T603 - Intégration de la passerelle de paiement sécurisée via API",
        "category": "6. Commandes et paiements",
        "column": "todo",
        "priority": 3,
        "week_offset": 5,
        "objective": "Assurer le règlement sécurisé de la commande via une passerelle de paiement d'API.",
        "description": (
            "Concevoir l'architecture passerelle de paiement : interface `PaymentGateway` et implémentation mockée réaliste "
            "`MockPaymentService` (supportant succès, carte refusée, fonds insuffisants, timeout réseau). "
            "Créer les endpoints REST `/api/paiement/intent` et `/api/paiement/confirm` sur cynemma-api, les intégrer dans "
            "le client JAR de l'API, et brancher le formulaire de checkout sécurisé sans stockage de carte bancaire."
        ),
        "subtasks": [
            "Concevoir l'interface PaymentGateway et l'implémentation simulée conforme PCI-DSS",
            "Créer les endpoints de paiement dans cynemma-api avec authentification M2M",
            "Ajouter les méthodes de paiement sécurisé dans le client JAR de l'API",
            "Développer le formulaire de checkout client (/checkout) avec validation Luhn",
            "Gérer les cas d'erreur de paiement avec messages clairs sans perdre le panier",
        ],
        "acceptance_criteria": [
            "Le client peut payer sa commande par carte bancaire.",
            "Une carte valide déclenche la capture et retourne une référence de transaction.",
            "Aucune donnée bancaire sensible n'est enregistrée dans la base de données locale.",
        ],
        "dependencies": [602, 701],
    },
    {
        "id_ref": 604,
        "title": "T604 - Finalisation transactionnelle de la commande et facturation",
        "category": "6. Commandes et paiements",
        "column": "todo",
        "priority": 3,
        "week_offset": 5,
        "objective": "Enregistrer la commande finale de manière transactionnelle après confirmation du paiement.",
        "description": (
            "Dès que le paiement est confirmé par l'API : convertir les verrous temporaires en enregistrements définitifs "
            "dans `reservations` et `billets`, enregistrer les lignes de commande confiserie, incrémenter les points "
            "sur la carte de fidélité du client, vider le panier en session et expédier l'email complet de confirmation avec facture."
        ),
        "subtasks": [
            "Exécuter la transaction JPA complète de finalisation (réservation, billets, snacks, fidélité)",
            "Vider les verrous temporaires et purger le panier de session",
            "Émettre l'email de confirmation récapitulatif avec numéro de commande et justificatif",
            "Rediriger vers la page de remerciement et confirmation d'achat",
        ],
        "acceptance_criteria": [
            "La commande est persistée de façon strictement atomique (tout ou rien).",
            "Les billets sont définitivement assignés au client.",
            "L'utilisateur reçoit instantanément sa confirmation par email et son récapitulatif écran.",
        ],
        "dependencies": [603],
    },

    # --------------------------------------------------------------------------
    # 7. SÉCURITÉ
    # --------------------------------------------------------------------------
    {
        "id_ref": 701,
        "title": "T701 - Sécurisation inter-projets par jeton JWT asymétrique RS256",
        "category": "7. Sécurité",
        "column": "done",
        "priority": 3,
        "week_offset": -2,
        "objective": "Sécuriser les communications Machine-to-Machine entre le WAR principal et l'API autonome.",
        "description": (
            "Authentification inter-WAR basée sur des jetons JWT signés par clé privée RSA 2048 bits côté cynemma "
            "(ApiJwtTokenService) et vérifiés par clé publique côté cynemma-api (JwtTokenVerifier). "
            "Contrôle strict des claims (émetteur, audience, rôle employé, expiration courte à 1 heure) "
            "et validation par la suite de tests automatisés dédiés."
        ),
        "subtasks": [
            "Générer la paire de clés RSA 2048 bits (jwt-private.pk8.der et jwt-public.x509.der)",
            "Implémenter ApiJwtTokenService pour signer les requêtes vers l'API",
            "Implémenter JwtTokenVerifier dans cynemma-api pour valider signature et claims",
            "Bloquer tout accès non autorisé avec HTTP 401 Unauthorized ou 403 Forbidden",
        ],
        "acceptance_criteria": [
            "Tout appel sur l'API interne sans en-tête Bearer JWT valide retourne HTTP 401.",
            "Un jeton falsifié, expiré ou signé avec un mauvais algorithme est rejeté.",
            "Les requêtes légitimes avec rôle employé sont acceptées.",
        ],
        "dependencies": [704],
    },
    {
        "id_ref": 702,
        "title": "T702 - Protection contre les attaques CSRF par jeton unique de session",
        "category": "7. Sécurité",
        "column": "done",
        "priority": 3,
        "week_offset": -2,
        "objective": "Protéger l'ensemble des formulaires et actions modifiantes contre les attaques Cross-Site Request Forgery.",
        "description": (
            "Implémentation de `CsrfProtection.java` avec génération de jetons cryptographiques aléatoires en session. "
            "Injection du jeton dans les vues sensibles et validation systématique dans les méthodes doPost() des servlets : "
            "BilletServlet, CreatSalleServlet, CreatSiegeServlet, DeleteUserServlet, RoleModifierServlet, CreatFilmServlet, "
            "EditFilmServlet, DeleteFilmServlet, CreatSeanceServlet, TmdbCatalogImportServlet et ChangePasswordServlet."
        ),
        "subtasks": [
            "Implémenter CsrfProtection.ensureToken() et CsrfProtection.isValid()",
            "Injecter le champ caché csrfToken dans tous les formulaires modifiants",
            "Vérifier et rejeter (HTTP 403 Forbidden) toute soumission POST sans jeton valide",
            "Migrer les actions d'administration déclenchées par GET vers des formulaires POST sécurisés",
        ],
        "acceptance_criteria": [
            "Toute requête POST sans jeton CSRF ou avec un jeton invalide renvoie une erreur 403.",
            "La soumission légitime depuis l'interface est acceptée sans friction.",
        ],
        "dependencies": [202],
    },
    {
        "id_ref": 703,
        "title": "T703 - Sécurisation des téléversements d'affiches et ré-encodage forcé",
        "category": "7. Sécurité",
        "column": "done",
        "priority": 3,
        "week_offset": -2,
        "objective": "Éliminer les risques d'exécution de code malveillant lors de l'upload d'affiches.",
        "description": (
            "Module `PosterStorage.java` : vérification stricte du format d'image (JPEG ou PNG uniquement), "
            "ré-encodage complet de l'image via ImageIO.read() et ImageIO.write() pour neutraliser tout code embarqué "
            "(polyglots, failles ImageMagick, scripts PHP/JSP camouflés), renommage par identifiant UUID aléatoire, "
            "stockage sur disque en dehors de la racine web du serveur et protection contre le Path Traversal."
        ),
        "subtasks": [
            "Implémenter la détection et validation des types MIME d'image autorisés",
            "Ré-encoder intégralement les pixels de l'image pour éradiquer les métadonnées malveillantes",
            "Attribuer un nom de fichier UUID sans conserver le nom fourni par le client",
            "Stocker les affiches dans un dossier externe configurable (CYNEMMA_POSTER_STORAGE_DIR)",
        ],
        "acceptance_criteria": [
            "Un fichier non-image ou une image corrompue est refusé avec une erreur 400 explicite.",
            "L'image ré-encodée est saine et débarrassée de tout payload exécutable.",
            "Aucun fichier uploadé ne peut être exécuté par le serveur web.",
        ],
        "dependencies": [802],
    },
    {
        "id_ref": 704,
        "title": "T704 - Confinement des secrets et purge des identifiants sensibles",
        "category": "7. Sécurité",
        "column": "done",
        "priority": 3,
        "week_offset": -3,
        "objective": "Empêcher toute fuite de clés API, mots de passe et certificats dans le dépôt Git.",
        "description": (
            "Révocation des anciennes clés de test. Confinement de la clé TMDB (TMDB_API_KEY), des identifiants SMTP, "
            "des accès BDD et des clés RSA privées dans le dossier `.local-secrets/` et les variables d'environnement système. "
            "Ajout des règles strictes dans `.gitignore` et lecture via la classe `LocalConfiguration`."
        ),
        "subtasks": [
            "Révocation de toute clé API externe ayant pu être exposée lors des phases de test",
            "Création de la hiérarchie de configuration sécurisée dans LocalConfiguration.java",
            "Confinement strict dans .local-secrets/ et exclusion formelle dans .gitignore",
            "Vérification de l'absence totale de secrets en clair dans le code source versionné",
        ],
        "acceptance_criteria": [
            "Aucun secret ni clé privée n'apparaît dans l'historique ou le code Git.",
            "L'application démarre et lit ses secrets depuis l'environnement ou .local-secrets/.",
        ],
        "dependencies": [],
    },
    {
        "id_ref": 705,
        "title": "T705 - Protection contre les doubles réservations par verrou pessimiste",
        "category": "7. Sécurité",
        "column": "done",
        "priority": 3,
        "week_offset": -1,
        "objective": "Garantir l'impossibilité absolue d'attribuer deux fois le même siège sur une séance.",
        "description": (
            "Double protection dans `BilletDAO.createReservation()` : verrouillage pessimiste en écriture au niveau JPA "
            "(`LockModeType.PESSIMISTIC_WRITE`) sur l'entité Seance, couplé à la contrainte d'unicité SQL absolue "
            "`UNIQUE KEY uq_billets_seance_siege (seance_id, siege_id)` au niveau MySQL InnoDB, et capture explicite "
            "du code d'erreur SQL 1062 (Duplicate entry)."
        ),
        "subtasks": [
            "Appliquer le verrou LockModeType.PESSIMISTIC_WRITE lors de la lecture de la séance",
            "Déclarer la contrainte d'unicité uq_billets_seance_siege dans le schéma relationnel",
            "Gérer l'exception SQLException 1062 et déclencher un rollback propre",
            "Valider la résistance aux accès simultanés par tests unitaires ciblés",
        ],
        "acceptance_criteria": [
            "Deux transactions simultanées tentant de réserver le même siège ne peuvent jamais aboutir toutes les deux.",
            "La base de données rejette la seconde tentative par contrainte d'unicité sans corruption.",
        ],
        "dependencies": [802],
    },
    {
        "id_ref": 706,
        "title": "T706 - Filtre de sécurité global Java EE (@WebFilter) sur les routes sensibles",
        "category": "7. Sécurité",
        "column": "todo",
        "priority": 3,
        "week_offset": 1,
        "objective": "Centraliser le contrôle d'accès dans un filtre servlet pour éliminer les risques d'omission.",
        "description": (
            "Actuellement, le contrôle de session et de rôle est réalisé manuellement au cas par cas dans chaque servlet, "
            "ce qui crée des risques de failles (comme le bug 'U' dans FilmListServlet). "
            "Créer un filtre global `SecurityFilter` annoté `@WebFilter` interceptant `/admin/*`, `/employe/*`, `/client/*` "
            "pour refuser immédiatement toute requête non autorisée avec journalisation de sécurité."
        ),
        "subtasks": [
            "Créer la classe SecurityFilter implémentant jakarta.servlet.Filter",
            "Configurer les motifs d'URL (/admin/*, /employe/*, /client/*, /api/internal/*)",
            "Vérifier la présence de la session et la concordance exacte du rôle requis",
            "Journaliser les tentatives d'accès non autorisées avec adresse IP et chemin visé",
        ],
        "acceptance_criteria": [
            "Toute route sous /admin/ est inaccessible sans session active avec rôle ADMINISTRATEUR.",
            "Toute route sous /employe/ exige le rôle EMPLOYE.",
            "L'oubli d'une vérification manuelle dans une servlet n'ouvre aucune faille de sécurité.",
        ],
        "dependencies": [202],
    },
    {
        "id_ref": 707,
        "title": "T707 - Protection contre la fixation de session et renforcement des cookies",
        "category": "7. Sécurité",
        "column": "todo",
        "priority": 2,
        "week_offset": 1,
        "objective": "Prévenir le vol de session lors de la connexion et sécuriser les attributs de cookies HTTP.",
        "description": (
            "Dans `AuthentificationServlet`, invoquer `request.changeSessionId()` immédiatement après la validation du mot de passe "
            "pour renouveler l'ID de session et contrer toute tentative de fixation de session. "
            "Configurer les cookies de session dans `web.xml` avec les drapeaux `HttpOnly`, `Secure` et `SameSite=Strict`."
        ),
        "subtasks": [
            "Ajouter request.changeSessionId() dans AuthentificationServlet lors d'un login réussi",
            "Configurer le bloc <cookie-config> dans web.xml (http-only, secure, same-site)",
            "Configurer un délai d'inactivité de session strict (30 minutes par défaut)",
        ],
        "acceptance_criteria": [
            "L'identifiant du cookie JSESSIONID change immédiatement après la connexion.",
            "Le cookie de session n'est pas accessible via document.cookie en JavaScript.",
        ],
        "dependencies": [202],
    },
    {
        "id_ref": 708,
        "title": "T708 - Politique de complexité des mots de passe et suppression du fallback en clair",
        "category": "7. Sécurité",
        "column": "todo",
        "priority": 2,
        "week_offset": 1,
        "objective": "Renforcer la solidité des mots de passe et éliminer tout risque d'authentification en clair.",
        "description": (
            "Supprimer la clause de repli permissive `plainPassword.equals(user.getMotDePasse())` dans `UserDAO.authenticate()`. "
            "Mettre en place une politique de mot de passe robuste lors de l'inscription et du changement : "
            "minimum 12 caractères, avec au moins une majuscule, une minuscule, un chiffre et un caractère spécial, "
            "validée côté client et obligatoirement contrôlée côté serveur par expression régulière."
        ),
        "subtasks": [
            "Purger la comparaison en clair de repli dans UserDAO.java",
            "Créer la classe utilitaire PasswordValidator avec regex de complexité conforme ANSSI",
            "Appliquer la validation dans CreatServlet et ChangePasswordServlet avec messages précis",
        ],
        "acceptance_criteria": [
            "Tout mot de passe de moins de 12 caractères ou manquant de diversité est rejeté.",
            "Aucun mot de passe en clair ne peut plus être validé par UserDAO.",
        ],
        "dependencies": [201, 204],
    },
    {
        "id_ref": 709,
        "title": "T709 - Limitation de débit (Rate Limiting) et protection anti-bruteforce",
        "category": "7. Sécurité",
        "column": "todo",
        "priority": 2,
        "week_offset": 3,
        "objective": "Protéger la mire de connexion et les API contre les attaques par force brute et par déni de service.",
        "description": (
            "Mettre en place un mécanisme de limitation du nombre de requêtes par adresse IP (Bucket4j ou filtre mémoire) "
            "sur la route `/connexion` (5 tentatives échouées max par 15 minutes avant verrouillage temporaire) "
            "et sur les endpoints de cynemma-api (limitation à 100 requêtes/minute avec retour HTTP 429 Too Many Requests)."
        ),
        "subtasks": [
            "Implémenter un limiteur de débit par adresse IP sur AuthentificationServlet",
            "Ajouter le retour HTTP 429 Too Many Requests avec en-tête Retry-After",
            "Mettre en place un compteur de tentatives de mot de passe échouées par compte",
        ],
        "acceptance_criteria": [
            "Au-delà de 5 tentatives infructueuses sur un compte, les requêtes sont temporairement bloquées.",
            "Les attaques par force brute distribuée sont freinées par la limitation IP.",
        ],
        "dependencies": [202, 706],
    },
    {
        "id_ref": 710,
        "title": "T710 - Headers de sécurité HTTP (CSP, HSTS, X-Content-Type, X-Frame-Options)",
        "category": "7. Sécurité",
        "column": "todo",
        "priority": 1,
        "week_offset": 3,
        "objective": "Élever le niveau de défense en profondeur du navigateur en injectant les en-têtes HTTP de sécurité.",
        "description": (
            "Créer un filtre HTTP injectant systématiquement sur toutes les réponses : "
            "`Content-Security-Policy` (restriction stricte des sources de scripts, styles, images et frames YouTube), "
            "`X-Frame-Options: DENY` (anti-clickjacking), `X-Content-Type-Options: nosniff`, "
            "`Referrer-Policy: strict-origin-when-cross-origin` et `Permissions-Policy`."
        ),
        "subtasks": [
            "Créer SecurityHeadersFilter pour injecter les en-têtes de sécurité recommandés",
            "Rédiger la directive CSP autorisant les polices Google Fonts et les iframes YouTube",
            "Interdire le rendu dans des cadres externes (anti-clickjacking)",
        ],
        "acceptance_criteria": [
            "Les scans de sécurité des en-têtes HTTP retournent une note maximale (A sur SecurityHeaders).",
            "Le chargement de ressources tierces non déclarées est bloqué par le navigateur.",
        ],
        "dependencies": [706],
    },

    # --------------------------------------------------------------------------
    # 8. INFRASTRUCTURE ET TESTS
    # --------------------------------------------------------------------------
    {
        "id_ref": 801,
        "title": "T801 - Suite de 67 tests unitaires automatisés (cynemma et API)",
        "category": "8. Infrastructure et tests",
        "column": "done",
        "priority": 2,
        "week_offset": -1,
        "objective": "Garantir la non-régression du code existant via une couverture de tests unitaires passants.",
        "description": (
            "Mise en place de 67 tests automatisés (`mvn test`) exécutés sans erreur : "
            "42 tests dans cynemma (BilletServlet, SeatLayoutCalculator, PosterStorage, TmdbCatalogImportServlet, "
            "ApiJwtTokenService, DeleteFilmServlet, etc.) et 25 tests dans API (TrailerServlet, InternalTmdbServlet, "
            "JwtTokenVerifier, LocalConfiguration, TmdbService). Couverture JaCoCo intégrée."
        ),
        "subtasks": [
            "Écrire et valider les 42 tests unitaires de l'application principale cynemma",
            "Écrire et valider les 25 tests unitaires du microservice cynemma-api",
            "Valider l'exécution conjointe via `mvn test` sans aucun échec ni saut de test",
            "Configurer le plugin JaCoCo pour mesurer la couverture de code",
        ],
        "acceptance_criteria": [
            "La commande `mvn test` dans les deux sous-dossiers retourne 'BUILD SUCCESS'.",
            "Les 67 tests s'exécutent en moins de 10 secondes.",
        ],
        "dependencies": [],
    },
    {
        "id_ref": 802,
        "title": "T802 - Configuration des DataSources JNDI MySQL et persistance JPA",
        "category": "8. Infrastructure et tests",
        "column": "done",
        "priority": 3,
        "week_offset": -3,
        "objective": "Établir la persistance relationnelle avec MySQL InnoDB et Hibernate 6.5.",
        "description": (
            "Configuration de `persistence.xml` (JPA 3.1 / Hibernate ORM 6.5), mapping des entités, pilote Connector/J 8.3, "
            "création du schéma relationnel `init_database.sql` avec contraintes CHECK, clés étrangères CASCADE "
            "et index de performance, et gestion du pool de connexions via JNDI `java:comp/env/jdbc/cynema`."
        ),
        "subtasks": [
            "Rédiger le script init_database.sql avec clés étrangères et contraintes CHECK MySQL 8",
            "Configurer persistence.xml avec le dialecte MySQL et Hibernate ORM",
            "Développer JPAUtil et le listener JPAContextListener pour gérer EntityManagerFactory",
            "Configurer la DataSource JNDI dans context.xml de Tomcat",
        ],
        "acceptance_criteria": [
            "L'EntityManagerFactory démarre correctement lors de l'initialisation du contexte.",
            "Toutes les tables et contraintes sont créées sans avertissement SQL.",
        ],
        "dependencies": [],
    },
    {
        "id_ref": 803,
        "title": "T803 - Tests d'intégration MySQL avec Testcontainers",
        "category": "8. Infrastructure et tests",
        "column": "todo",
        "priority": 2,
        "week_offset": 6,
        "objective": "Valider le bon fonctionnement des DAO et transactions JPA sur une véritable instance MySQL.",
        "description": (
            "Les tests unitaires actuels utilisent des mocks et ne testent pas les requêtes SQL réelles. "
            "Intégrer la bibliothèque Testcontainers (module testcontainers-mysql) pour lancer un conteneur MySQL éphémère "
            "pendant `mvn verify` et valider les requêtes JPQL/natives, les contraintes CHECK et les rollbacks réels."
        ),
        "subtasks": [
            "Ajouter les dépendances Testcontainers et JUnit 5 dans pom.xml",
            "Créer la classe de base MySQLIntegrationTest levant un conteneur MySQL 8",
            "Tester l'ensemble des méthodes de FilmDAO, SeanceDAO, SalleDAO, BilletDAO et UserDAO sur la vraie base",
            "Valider le comportement réel des contraintes d'unicité et clés étrangères",
        ],
        "acceptance_criteria": [
            "La commande `mvn verify` exécute la suite d'intégration sur un conteneur Docker MySQL.",
            "Toutes les méthodes de persistance et transactions JPA sont validées en conditions réelles.",
        ],
        "dependencies": [801, 802],
    },
    {
        "id_ref": 804,
        "title": "T804 - Tests de concurrence et résistance aux deadlocks",
        "category": "8. Infrastructure et tests",
        "column": "todo",
        "priority": 2,
        "week_offset": 6,
        "objective": "Vérifier la robustesse du système lors de réservations massives et simultanées.",
        "description": (
            "Développer un banc d'essai multi-threadé simulant 50 clients tentant simultanément de réserver le même siège "
            "ou les mêmes places sur une séance très demandée. Valider l'étanchéité du verrou pessimiste, l'absence de deadlocks "
            "bloquant les connexions MySQL et le comportement de la gestion d'erreur SQL 1062."
        ),
        "subtasks": [
            "Créer un test de charge multi-threads avec ExecutorService et CountDownLatch",
            "Simuler des réservations concurrentes agressives sur une séance unique",
            "Vérifier qu'exactement une réservation aboutit et que les 49 autres échouent proprement",
            "Vérifier l'absence de fuite de connexions dans le pool JNDI",
        ],
        "acceptance_criteria": [
            "Aucune double réservation n'est possible, même sous forte concurrence.",
            "Le pool de connexions reste stable et aucun verrou orphelin ne subsiste en base.",
        ],
        "dependencies": [705, 803],
    },
    {
        "id_ref": 805,
        "title": "T805 - Tests end-to-end (E2E) automatisés avec Playwright",
        "category": "8. Infrastructure et tests",
        "column": "todo",
        "priority": 1,
        "week_offset": 7,
        "objective": "Automatiser le parcours utilisateur complet dans un vrai navigateur web.",
        "description": (
            "Mettre en place une suite de tests de bout en bout avec Playwright (Java ou Python) couvrant les 4 parcours : "
            "Visiteur (consultation films), Client (inscription, connexion, choix séance, réservation, téléchargement billet), "
            "Employé (import TMDB, création de séance) et Administrateur (création salle, gestion des comptes)."
        ),
        "subtasks": [
            "Initialiser le projet de tests Playwright",
            "Rédiger le scénario E2E complet du parcours client (achat d'une place)",
            "Rédiger le scénario employé (recherche et import d'un film TMDB)",
            "Rédiger le scénario administrateur (création d'une salle et modification de rôle)",
        ],
        "acceptance_criteria": [
            "Les parcours critiques s'exécutent automatiquement dans un navigateur Chromium sans régression visuelle ni blocage.",
        ],
        "dependencies": [103, 406, 506],
    },

    # --------------------------------------------------------------------------
    # 9. DOCUMENTATION ET DÉPLOIEMENT
    # --------------------------------------------------------------------------
    {
        "id_ref": 901,
        "title": "T901 - Documentation d'architecture technique et fonctionnement API",
        "category": "9. Documentation et déploiement",
        "column": "done",
        "priority": 1,
        "week_offset": -1,
        "objective": "Documenter l'architecture multi-modules, les contrats d'API et le guide d'installation.",
        "description": (
            "Rédaction des documents techniques majeurs du projet : `README.md` (présentation, technologies, matrice des rôles, "
            "architecture MVC, limites connues), `EXPLICATION_FONCTIONNEMENT_API.md` (fonctionnement détaillé de l'API /api/trailer, "
            "cache MySQL, service TMDB) et `API/PLAN_TESTS.md` (stratégie et couverture des tests automatisés)."
        ),
        "subtasks": [
            "Rédiger le README.md exhaustif avec schémas Mermaid et guide d'installation",
            "Rédiger EXPLICATION_FONCTIONNEMENT_API.md décrivant le cache et les flux HTTP",
            "Documenter la stratégie de test et les limites actuelles dans API/PLAN_TESTS.md",
        ],
        "acceptance_criteria": [
            "La documentation technique décrit fidèlement l'état réel du code.",
            "Un nouveau développeur peut installer et démarrer les deux WAR grâce au guide.",
        ],
        "dependencies": [],
    },
    {
        "id_ref": 902,
        "title": "T902 - Déploiement multi-WAR sur serveur Tomcat 11",
        "category": "9. Documentation et déploiement",
        "column": "done",
        "priority": 3,
        "week_offset": -1,
        "objective": "Compiler et déployer les deux archives web (cynemma.war et cynemma-api.war) sur Tomcat.",
        "description": (
            "Configuration des fichiers pom.xml (Java 21, packaging WAR, Jakarta EE 10 / Servlet 6.1). "
            "Assemblage via Maven et déploiement sur Apache Tomcat 11 : cynemma.war sur le port 8080 "
            "et cynemma-api.war sur le port 8080 (ou 8081). Validation du dialogue HTTP entre les deux applications."
        ),
        "subtasks": [
            "Configurer le build Maven pour générer cynemma.war et cynemma-api.war",
            "Configurer les contextes Tomcat et les variables d'environnement de communication",
            "Tester l'appel du WAR principal vers la route /api/health et /api/trailer du WAR API",
        ],
        "acceptance_criteria": [
            "Les deux archives WAR se déploient sans erreur de démarrage sur Tomcat 11.",
            "L'endpoint de santé /api/health répond HTTP 200 OK.",
        ],
        "dependencies": [802],
    },
    {
        "id_ref": 903,
        "title": "T903 - Configuration HTTPS et isolation réseau du microservice API",
        "category": "9. Documentation et déploiement",
        "column": "todo",
        "priority": 2,
        "week_offset": 7,
        "objective": "Chiffrer le trafic en HTTPS et confiner le WAR API sur un réseau privé interne.",
        "description": (
            "Mettre en place un reverse proxy Nginx ou configuration de connecteur Tomcat SSL/TLS avec certificat. "
            "Bloquer tout accès direct de l'Internet public vers les endpoints `/api/internal/*` du microservice API, "
            "en autorisant uniquement les appels locaux (loopback 127.0.0.1) ou réseau privé provenant de l'application Cynemma."
        ),
        "subtasks": [
            "Configurer le certificat SSL/TLS et forcer la redirection HTTP vers HTTPS",
            "Configurer les règles de pare-feu ou reverse-proxy pour cloisonner cynemma-api",
            "Vérifier le rejet des requêtes externes sur les routes sensibles internes",
        ],
        "acceptance_criteria": [
            "Toutes les communications publiques transitent en HTTPS avec certificat valide.",
            "L'API interne n'est pas joignable depuis l'extérieur du réseau privé.",
        ],
        "dependencies": [902, 701],
    },
    {
        "id_ref": 904,
        "title": "T904 - Guide de déploiement en production et supervision système",
        "category": "9. Documentation et déploiement",
        "column": "todo",
        "priority": 1,
        "week_offset": 7,
        "objective": "Fournir les procédures d'exploitation, de sauvegarde et de surveillance en production.",
        "description": (
            "Rédiger le manuel d'exploitation de production : procédure de déploiement sans interruption de service, "
            "script de sauvegarde quotidienne de la base de données MySQL et des affiches uploadées, "
            "surveillance des métriques JVM, journalisation centralisée et alertes en cas d'erreurs critiques."
        ),
        "subtasks": [
            "Rédiger la procédure de mise en production et de rollback",
            "Fournir un script de sauvegarde automatique de MySQL et du dossier de stockage des affiches",
            "Documenter la surveillance du pool de connexions JNDI et des logs applicatifs",
        ],
        "acceptance_criteria": [
            "L'exploitant dispose d'une documentation complète pour opérer et sauvegarder le service.",
        ],
        "dependencies": [903],
    },
]


# ==============================================================================
# CLIENT JSON-RPC KANBOARD ROBUSTE
# ==============================================================================

class KanboardAPIError(RuntimeError):
    """Exception levée en cas d'erreur de communication ou réponse invalide de Kanboard."""


class KanboardClient:
    """Client JSON-RPC 2.0 pour l'API Kanboard."""

    def __init__(self, url: str, username: str, token: str) -> None:
        try:
            import requests
        except ModuleNotFoundError as exc:
            raise KanboardAPIError(
                "Dépendance manquante : installez requests avec `pip install requests`."
            ) from exc

        self.requests = requests
        self.url = url
        self.session = requests.Session()
        self.session.auth = (username, token)
        self.request_id = 0

    def call(self, method: str, params: Optional[dict[str, Any]] = None) -> Any:
        self.request_id += 1
        payload = {
            "jsonrpc": "2.0",
            "method": method,
            "id": self.request_id,
            "params": params or {},
        }
        try:
            response = self.session.post(
                self.url,
                json=payload,
                timeout=REQUEST_TIMEOUT_SECONDS,
            )
            response.raise_for_status()
            body = response.json()
        except ValueError as exc:
            raise KanboardAPIError(
                f"Réponse Kanboard non JSON pour {method} : {exc}"
            ) from exc
        except self.requests.exceptions.RequestException as exc:
            raise KanboardAPIError(
                f"Erreur de communication avec Kanboard ({method}) : {exc}"
            ) from exc

        if not isinstance(body, dict):
            raise KanboardAPIError("Format de réponse JSON-RPC invalide (objet attendu).")
        if "error" in body:
            raise KanboardAPIError(f"Erreur API pour {method} : {body['error']}")
        if "result" not in body:
            raise KanboardAPIError(f"Réponse JSON-RPC sans champ résultat pour {method}.")
        return body["result"]


class SnapshotKanboardClient:
    """Simulateur en mémoire pour tester le script hors ligne depuis une sauvegarde JSON."""

    def __init__(self, snapshot_data: dict[str, Any]) -> None:
        self.project_id = int(snapshot_data.get("project_id", 1))
        self.project_name = snapshot_data.get("project_name", "Cinema")
        self.columns = snapshot_data.get("columns", [])
        self.tasks = snapshot_data.get("tasks", [])
        self.categories: list[dict[str, Any]] = []
        self.links: list[dict[str, Any]] = []
        self.next_id = 1000

    def call(self, method: str, params: Optional[dict[str, Any]] = None) -> Any:
        params = params or {}
        if method == "getProjectByName":
            name = params.get("name", "").strip()
            if name.lower() in {self.project_name.lower().strip(), "cinema"}:
                return {"id": self.project_id, "name": self.project_name}
            return False
        if method == "getColumns":
            return self.columns
        if method == "getAllTasks":
            return [t.get("task", t) for t in self.tasks]
        if method == "getAllSubtasks":
            return []
        if method == "getAllTaskLinks":
            return []
        if method == "getAllLinkLabels":
            return [{"id": 1, "label": "is blocked by"}]
        if method == "getCategoryList":
            return {cat["id"]: cat["name"] for cat in self.categories}
        if method == "createCategory":
            self.next_id += 1
            cat_id = self.next_id
            self.categories.append({"id": cat_id, "name": params.get("name")})
            return cat_id
        if method == "createTask":
            self.next_id += 1
            return self.next_id
        if method == "createSubtask":
            return True
        if method == "createTaskLink":
            return True
        if method == "removeTask":
            return True
        if method == "updateTask":
            return True
        if method == "moveTaskPosition":
            return True
        return True


# ==============================================================================
# FONCTIONS UTILITAIRES DE GESTION
# ==============================================================================

def normalize_text(value: str) -> str:
    """Normalise une chaîne pour comparaison insensible à la casse et aux accents."""
    without_accents = unicodedata.normalize("NFKD", str(value))
    return "".join(c for c in without_accents if c.isalnum()).lower()


def compute_week_dates(base_date: date, week_offset: int) -> tuple[str, str]:
    """Calcule les dates de début (lundi 09:00) et fin (vendredi 17:00) pour une semaine offset."""
    current_monday = base_date - timedelta(days=base_date.weekday())
    target_monday = current_monday + timedelta(weeks=week_offset)
    target_friday = target_monday + timedelta(days=4)
    start_str = datetime.combine(target_monday, datetime.min.time()).replace(hour=9).strftime("%Y-%m-%d %H:%M")
    due_str = datetime.combine(target_friday, datetime.min.time()).replace(hour=17).strftime("%Y-%m-%d %H:%M")
    return start_str, due_str


def find_project(client: Any, project_name: str) -> dict[str, Any]:
    """Recherche le projet Kanboard par son nom ou variantes d'espaces."""
    for candidate in [project_name, project_name.strip(), f"{project_name.strip()} "]:
        res = client.call("getProjectByName", {"name": candidate})
        if isinstance(res, dict) and "id" in res:
            return res
    raise KanboardAPIError(f"Projet Kanboard « {project_name} » introuvable.")


def resolve_columns(client: Any, project_id: int) -> dict[str, int]:
    """Identifie et valide les colonnes du tableau Kanboard."""
    raw_columns = client.call("getColumns", {"project_id": project_id})
    if not isinstance(raw_columns, list) or not raw_columns:
        raise KanboardAPIError("Impossible de charger les colonnes du projet Kanboard.")

    resolved: dict[str, int] = {}
    for col_key, aliases in COLUMN_ALIASES.items():
        preferred_title = COLUMN_LABELS[col_key]
        exact_matches = [
            c for c in raw_columns
            if isinstance(c, dict) and normalize_text(c.get("title", "")) == normalize_text(preferred_title)
        ]
        alias_matches = exact_matches or [
            c for c in raw_columns
            if isinstance(c, dict) and normalize_text(c.get("title", "")) in aliases
        ]
        if alias_matches:
            resolved[col_key] = int(alias_matches[0]["id"])
        else:
            # Fallback gracieux si une colonne spécifique n'existe pas : reporter sur todo ou premier dispo
            if "todo" in resolved:
                resolved[col_key] = resolved["todo"]
            elif raw_columns:
                resolved[col_key] = int(raw_columns[0]["id"])

    # Garantir que todo, in_progress, done sont toujours définis
    for req in ["todo", "in_progress", "done"]:
        if req not in resolved:
            raise KanboardAPIError(f"Impossible de résoudre la colonne obligatoire « {COLUMN_LABELS[req]} ».")
    if "backlog" not in resolved:
        resolved["backlog"] = resolved["todo"]

    return resolved


def ensure_categories(client: Any, project_id: int) -> dict[str, int]:
    """Crée ou résout les 9 catégories officielles dans Kanboard."""
    category_map: dict[str, int] = {}
    try:
        raw_list = client.call("getCategoryList", {"project_id": project_id})
        existing: dict[str, int] = {}
        if isinstance(raw_list, dict):
            for cid, cname in raw_list.items():
                existing[normalize_text(cname)] = int(cid)
        elif isinstance(raw_list, list):
            for c in raw_list:
                if isinstance(c, dict) and "id" in c and "name" in c:
                    existing[normalize_text(c["name"])] = int(c["id"])

        for category_name in CATEGORIES:
            norm = normalize_text(category_name)
            if norm in existing:
                category_map[category_name] = existing[norm]
            else:
                try:
                    new_id = client.call("createCategory", {
                        "project_id": project_id,
                        "name": category_name,
                    })
                    if isinstance(new_id, int):
                        category_map[category_name] = new_id
                except KanboardAPIError:
                    pass
    except KanboardAPIError:
        pass
    return category_map


def resolve_blocked_by_link_id(client: Any) -> Optional[int]:
    """Recherche l'identifiant du type de lien « is blocked by »."""
    try:
        labels = client.call("getAllLinkLabels")
        if isinstance(labels, list):
            for link in labels:
                if isinstance(link, dict):
                    label = str(link.get("label", "")).lower()
                    if "blocked by" in label or "bloqu" in label:
                        return int(link["id"])
    except KanboardAPIError:
        pass
    return None


def create_full_backup(client: Any, project_id: int, project_name: str) -> str:
    """Effectue une sauvegarde intégrale du projet dans un fichier JSON horodaté."""
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    sanitized_name = "".join(c for c in project_name if c.isalnum() or c in ("-", "_")).strip()
    backup_filename = f"kanboard_backup_{sanitized_name}_{timestamp}.json"
    backup_path = os.path.join(os.path.dirname(__file__), backup_filename)

    columns = client.call("getColumns", {"project_id": project_id})
    raw_tasks = client.call("getAllTasks", {"project_id": project_id}) or []

    detailed_tasks: list[dict[str, Any]] = []
    for t in raw_tasks:
        tid = int(t["id"])
        subtasks = []
        links = []
        try:
            subtasks = client.call("getAllSubtasks", {"task_id": tid}) or []
        except KanboardAPIError:
            pass
        try:
            links = client.call("getAllTaskLinks", {"task_id": tid}) or []
        except KanboardAPIError:
            pass
        detailed_tasks.append({
            "task": t,
            "subtasks": subtasks,
            "links": links,
        })

    backup_payload = {
        "timestamp": datetime.now().isoformat(),
        "project_id": project_id,
        "project_name": project_name,
        "columns": columns,
        "tasks_count": len(detailed_tasks),
        "tasks": detailed_tasks,
    }

    with open(backup_path, "w", encoding="utf-8") as f:
        json.dump(backup_payload, f, indent=2, ensure_ascii=False)

    print(f"  [✓] Sauvegarde complète enregistrée : {backup_filename} ({len(detailed_tasks)} tâches)")
    return backup_path


def format_task_description(item: dict[str, Any]) -> str:
    """Formate une description technique Markdown riche et professionnelle."""
    sections = [
        f"### 🎯 Objectif\n{item['objective']}\n",
        f"### 📋 Description technique\n{item['description']}\n",
        "### 🧩 Sous-tâches techniques",
    ]
    for sub in item["subtasks"]:
        sections.append(f"- [ ] {sub}")

    sections.append("\n### ✅ Critères d'acceptation (Definition of Done)")
    for ac in item["acceptance_criteria"]:
        sections.append(f"- [ ] {ac}")

    deps = item.get("dependencies", [])
    if deps:
        deps_str = ", ".join(f"T{d}" for d in deps)
        sections.append(f"\n### 🔗 Dépendances requises : {deps_str}")
    else:
        sections.append("\n### 🔗 Dépendances : Aucune (Tâche initiale / autonome)")

    return "\n".join(sections) + "\n"


# ==============================================================================
# AFFICHAGE ET RAPPORT DU PLAN
# ==============================================================================

def display_plan(
    project_name: str,
    existing_tasks: list[dict[str, Any]],
    backlog: list[dict[str, Any]],
    column_ids: dict[str, int],
    base_date: date,
) -> None:
    """Affiche un aperçu complet du plan d'exécution sans modification."""
    print("=" * 80)
    print(f"PLAN DU BACKLOG KANBOARD - PROJET : {project_name.upper()}")
    print("=" * 80)
    print(f"Date de référence : {base_date.strftime('%d/%m/%Y')}")
    print(f"Tâches existantes sur Kanboard : {len(existing_tasks)}")
    print(f"Tâches du backlog à injecter   : {len(backlog)}")
    print(f"Sous-tâches prévues            : {sum(len(t['subtasks']) for t in backlog)}")

    status_counts = {"done": 0, "in_progress": 0, "todo": 0, "backlog": 0}
    for item in backlog:
        col = item["column"]
        status_counts[col] = status_counts.get(col, 0) + 1

    print("\nRépartition par statut réel :")
    print(f"  - Terminé (Done)            : {status_counts['done']}")
    print(f"  - En cours (In progress)    : {status_counts['in_progress']}")
    print(f"  - À faire (TO DO / Backlog) : {status_counts['todo'] + status_counts['backlog']}")

    current_cat = ""
    for item in backlog:
        if item["category"] != current_cat:
            current_cat = item["category"]
            print(f"\n--- {current_cat.upper()} ---")

        start_dt, due_dt = compute_week_dates(base_date, item["week_offset"])
        col_lbl = COLUMN_LABELS.get(item["column"], item["column"].upper())
        deps_str = f" [Dépend de : {', '.join(f'T{d}' for d in item['dependencies'])}]" if item.get("dependencies") else ""
        print(f"  [{col_lbl:^16}] CYN-{item['id_ref']:<4} : {item['title']}")
        print(f"                   Dates: {start_dt[:10]} -> {due_dt[:10]} | Priorité: {item['priority']}{deps_str}")
        for sub in item["subtasks"]:
            print(f"                     • {sub}")

    print("\n" + "=" * 80)


# ==============================================================================
# EXÉCUTION DE L'INJECTION ET POPULATION
# ==============================================================================

def execute_populate(
    client: Any,
    project_id: int,
    project_name: str,
    backlog: list[dict[str, Any]],
    column_ids: dict[str, int],
    category_ids: dict[str, int],
    blocked_by_link_id: Optional[int],
    existing_tasks: list[dict[str, Any]],
    reset: bool,
    base_date: date,
) -> None:
    """Exécute l'injection du backlog avec vérification post-création."""
    print("\n[1/4] Sauvegarde de sécurité...")
    backup_path = create_full_backup(client, project_id, project_name)

    if reset and existing_tasks:
        print(f"\n[2/4] Suppression contrôlée des {len(existing_tasks)} tâches existantes...")
        for t in existing_tasks:
            tid = int(t["id"])
            client.call("removeTask", {"task_id": tid})
            print(f"  [-] Tâche supprimée ID {tid} : {t.get('title', '')}")
    else:
        print(f"\n[2/4] Mode ajout/mise à jour conservateur ({len(existing_tasks)} existantes conservées)...")

    print(f"\n[3/4] Création des {len(backlog)} tâches et de leurs sous-tâches...")
    id_ref_to_kanboard_id: dict[int, int] = {}
    created_count = 0
    subtasks_count = 0

    for item in backlog:
        col_id = column_ids.get(item["column"], column_ids["todo"])
        cat_id = category_ids.get(item["category"], 0)
        start_date, due_date = compute_week_dates(base_date, item["week_offset"])
        description = format_task_description(item)

        task_payload = {
            "project_id": project_id,
            "title": item["title"],
            "description": description,
            "column_id": col_id,
            "reference": f"CYN-{item['id_ref']}",
            "priority": item["priority"],
            "date_started": start_date,
            "date_due": due_date,
        }
        if cat_id:
            task_payload["category_id"] = cat_id

        created_res = client.call("createTask", task_payload)
        if isinstance(created_res, bool) or not isinstance(created_res, (int, str)):
            raise KanboardAPIError(f"Échec de création pour la tâche « {item['title']} » (retour: {created_res}).")

        kanboard_task_id = int(created_res)
        id_ref_to_kanboard_id[item["id_ref"]] = kanboard_task_id
        created_count += 1
        print(f"  [+] Créée [ID {kanboard_task_id}] : {item['title']}")

        for subtask_title in item["subtasks"]:
            client.call("createSubtask", {
                "task_id": kanboard_task_id,
                "title": subtask_title,
            })
            subtasks_count += 1

    print(f"\n[4/4] Création des liens de dépendance entre tâches...")
    links_created = 0
    if blocked_by_link_id:
        for item in backlog:
            current_kb_id = id_ref_to_kanboard_id.get(item["id_ref"])
            if not current_kb_id:
                continue
            for dep_ref in item.get("dependencies", []):
                dep_kb_id = id_ref_to_kanboard_id.get(dep_ref)
                if dep_kb_id and current_kb_id != dep_kb_id:
                    try:
                        client.call("createTaskLink", {
                    "task_id": current_kb_id,
                            "opposite_task_id": dep_kb_id,
                            "link_id": blocked_by_link_id,
                        })
                        links_created += 1
                    except KanboardAPIError as exc:
                        print(f"  [!] Avertissement lien T{item['id_ref']} -> T{dep_ref} : {exc}")
    else:
        print("  [!] Type de lien « is blocked by » introuvable ; liens omis.")

    print("\n" + "=" * 80)
    print("VÉRIFICATION POST-CRÉATION ET SUCCÈS")
    print("=" * 80)
    print(f"✓ Tâches créées avec succès   : {created_count}")
    print(f"✓ Sous-tâches enregistrées    : {subtasks_count}")
    print(f"✓ Liens de dépendance créés   : {links_created}")
    print(f"✓ Sauvegarde préalable        : {backup_path}")
    print("Backlog complet injecté et opérationnel sur Kanboard.")


# ==============================================================================
# POINT D'ENTRÉE CLI
# ==============================================================================

def parse_arguments() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Gestionnaire complet du backlog Kanboard du projet Cinema (Cynemma)."
    )
    parser.add_argument(
        "--apply",
        action="store_true",
        help="Appliquer réellement les modifications (sauvegarde préalable et confirmation requise).",
    )
    parser.add_argument(
        "--reset",
        action="store_true",
        help="Supprimer les tâches existantes du projet avant injection (nécessite --apply).",
    )
    parser.add_argument(
        "--yes",
        action="store_true",
        help="Confirmer sans invite interactive (uniquement autorisé avec --apply).",
    )
    parser.add_argument(
        "--snapshot",
        help="Tester localement en simulation à partir d'un fichier de sauvegarde JSON.",
    )
    parser.add_argument(
        "--url",
        default=DEFAULT_KANBOARD_URL,
        help="URL de l'API JSON-RPC Kanboard (ex: http://localhost:8080/jsonrpc.php).",
    )
    parser.add_argument(
        "--token",
        default=DEFAULT_API_TOKEN,
        help="Jeton API Kanboard.",
    )
    parser.add_argument(
        "--user",
        default=DEFAULT_API_USERNAME,
        help="Nom d'utilisateur API Kanboard (par défaut: jsonrpc).",
    )
    parser.add_argument(
        "--project",
        default=DEFAULT_PROJECT_NAME,
        help="Nom du projet Kanboard ciblé (par défaut: Cinema).",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_arguments()

    if args.yes and not args.apply:
        print("Erreur : --yes ne peut être utilisé qu'avec --apply.", file=sys.stderr)
        return 2

    # Mode snapshot (simulation locale)
    if args.snapshot:
        print(f"=== MODE SIMULATION LOCALE DEPUIS SNAPSHOT : {args.snapshot} ===")
        try:
            with open(args.snapshot, "r", encoding="utf-8") as f:
                snap_data = json.load(f)
            client: Any = SnapshotKanboardClient(snap_data)
            project_id = client.project_id
            project_name = client.project_name
        except Exception as e:
            print(f"Erreur lors de la lecture du snapshot : {e}", file=sys.stderr)
            return 1
    else:
        # Résolution des identifiants
        secrets = load_local_env_secrets()
        url = args.url or secrets.get("KANBOARD_URL", "")
        token = args.token or secrets.get("KANBOARD_API_TOKEN", "")
        username = args.user or secrets.get("KANBOARD_API_USERNAME", "jsonrpc")
        project_name = args.project or secrets.get("KANBOARD_PROJECT_NAME", "Cinema")

        if not url or "example.com" in url or not token or token.startswith("REMPLACEZ"):
            print("=" * 80)
            print("CONFIGURATION KANBOARD MANQUANTE")
            print("=" * 80)
            print("Veuillez configurer vos identifiants Kanboard :")
            print("  1. Variables d'environnement : KANBOARD_URL et KANBOARD_API_TOKEN")
            print("  2. Arguments CLI : --url <url> --token <token>")
            print("  3. Fichier local : .local-secrets/cynemma.env")
            print("  4. Ou testez immédiatement hors ligne avec : --snapshot <sauvegarde.json>")
            print("=" * 80)
            # Si mode interactif disponible, demander
            if sys.stdin.isatty():
                try:
                    url = url or input("Entrez l'URL JSON-RPC Kanboard : ").strip()
                    token = token or getpass.getpass("Entrez le jeton API Kanboard : ").strip()
                except (EOFError, KeyboardInterrupt):
                    return 2
            if not url or not token:
                return 2

        client = KanboardClient(url, username, token)
        try:
            proj = find_project(client, project_name)
            project_id = int(proj["id"])
            project_name = proj.get("name", project_name)
        except KanboardAPIError as e:
            print(f"Erreur connexion projet : {e}", file=sys.stderr)
            return 1

    try:
        column_ids = resolve_columns(client, project_id)
        existing_tasks = client.call("getAllTasks", {"project_id": project_id}) or []
        category_ids = ensure_categories(client, project_id)
        blocked_by_link = resolve_blocked_by_link_id(client)

        display_plan(project_name, existing_tasks, BACKLOG_DATA, column_ids, REFERENCE_DATE)

        if not args.apply:
            print("\n[MODE APERÇU SÉCURISÉ] Aucune modification n'a été apportée à Kanboard.")
            print("Pour appliquer réellement ce plan avec sauvegarde préalable, relancez avec :")
            print(f"    python3 {os.path.basename(__file__)} --apply" + (" --reset" if args.reset else ""))
            return 0

        # Confirmation interactive obligatoire avant action destructive
        if args.reset:
            confirm_phrase = f"SUPPRIMER ET REMPLACER {project_name.strip()}"
            if not args.yes:
                print("\n" + "!" * 80)
                print("ATTENTION : VOUS AVEZ DEMANDÉ LE REMPLACEMENT INTÉGRAL DU BACKLOG.")
                print(f"Toutes les {len(existing_tasks)} tâches actuelles seront archivées en sauvegarde puis purgées.")
                print("!" * 80)
                try:
                    rep = input(f"Tapez exactement « {confirm_phrase} » pour continuer : ").strip()
                except (EOFError, KeyboardInterrupt):
                    print("\nAnnulé par l'utilisateur.")
                    return 1
                if rep != confirm_phrase:
                    print("Confirmation invalide ; aucune modification n'a été effectuée.")
                    return 1

        execute_populate(
            client=client,
            project_id=project_id,
            project_name=project_name,
            backlog=BACKLOG_DATA,
            column_ids=column_ids,
            category_ids=category_ids,
            blocked_by_link_id=blocked_by_link,
            existing_tasks=existing_tasks,
            reset=args.reset,
            base_date=REFERENCE_DATE,
        )
        return 0

    except KanboardAPIError as e:
        print(f"\n[!] Erreur Kanboard : {e}", file=sys.stderr)
        return 1
    except Exception as e:
        print(f"\n[!] Erreur imprévue : {e}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
