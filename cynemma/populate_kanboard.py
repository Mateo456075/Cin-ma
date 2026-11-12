#!/usr/bin/env python3
"""Reset the Kanboard project "Cinema" and populate it with the requested backlog."""

import argparse
import os
import sys
import unicodedata
from datetime import date, datetime, timedelta
from typing import Any, Optional

# Configure these values here, or override them with environment variables.
KANBOARD_URL = os.getenv(
    "KANBOARD_URL", "https://kanboard.example.com/jsonrpc.php"
)
API_USERNAME = os.getenv("KANBOARD_API_USERNAME", "jsonrpc")
API_TOKEN = os.getenv("KANBOARD_API_TOKEN", "REMPLACEZ_PAR_VOTRE_TOKEN")
PROJECT_NAME = "Cinema"
REQUEST_TIMEOUT_SECONDS = 30


BACKLOG = (
    {
        "title": "Déploiement Serveur & Config BDD",
        "column": "done",
        "week_offset": -1,
        "subtasks": ("Maj pom.xml", "Config Hibernate", "Test MAMP"),
    },
    {
        "title": "Résolution des Conflits de Routage MVC",
        "column": "done",
        "week_offset": -1,
        "subtasks": ("Séparer /listeSeances", "Tester vues Client/Employé"),
    },
    {
        "title": "Sécurisation Faille Upload (Image Magick)",
        "column": "done",
        "week_offset": -1,
        "subtasks": ("Contrôle du rôle POST", "Ré-encodage forcé PNG"),
    },
    {
        "title": "Sécurisation Faille CSRF",
        "column": "done",
        "week_offset": -1,
        "subtasks": ("Créer CsrfProtection.java", "Transformer liens GET en POST"),
    },
    {
        "title": "Architecture d'Authentification Centralisée",
        "column": "done",
        "week_offset": -1,
        "subtasks": ("Créer SessionRegistry", "Rediriger login vers accueil (/)"),
    },
    {
        "title": "Refactoring UI avec Header Dynamique",
        "column": "in_progress",
        "week_offset": 0,
        "subtasks": ("Inclure JSTL", "Affichage conditionnel selon typeUtilisateur"),
    },
    {
        "title": "Règle Métier : Anti-Chevauchement Séances",
        "column": "in_progress",
        "week_offset": 0,
        "subtasks": (
            "Méthode JPQL verifierDisponibilite",
            "Bouclier dans Servlet",
            "Affichage message erreur",
        ),
    },
    {
        "title": "Sécurité Globale (Filtre Java EE)",
        "column": "todo",
        "week_offset": 1,
        "subtasks": ("Créer @WebFilter", "Appliquer sur dossiers /admin et /employe"),
    },
    {
        "title": "Développement API REST (Backend)",
        "column": "todo",
        "week_offset": 1,
        "subtasks": ("Ajouter Gson au pom.xml", "Créer ApiFilmServlet (/api/films)"),
    },
    {
        "title": "Consommation API REST (Frontend)",
        "column": "todo",
        "week_offset": 1,
        "subtasks": (
            "Script JS fetch()",
            "Injection DOM asynchrone pour vidéos",
        ),
    },
)

COLUMN_ALIASES = {
    "done": {"termine", "terminee", "done", "completed"},
    "in_progress": {
        "encours",
        "inprogress",
        "workinprogress",
        "doing",
    },
    "todo": {"afaire", "todo", "backlog", "ready"},
}
COLUMN_LABELS = {
    "done": "Terminé",
    "in_progress": "En Cours",
    "todo": "À Faire",
}


class KanboardAPIError(RuntimeError):
    """Raised when Kanboard returns an API or transport error."""


class KanboardClient:
    def __init__(self, url: str, username: str, token: str) -> None:
        try:
            import requests
        except ModuleNotFoundError as exc:
            raise KanboardAPIError(
                "Dépendance manquante : installez requests avec "
                "`python3 -m pip install requests`."
            ) from exc

        self.requests = requests
        self.url = url
        self.session = requests.Session()
        self.session.auth = (username, token)
        self.request_id = 0

    def call(
        self, method: str, params: Optional[dict[str, Any]] = None
    ) -> Any:
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
                "Kanboard a renvoyé une réponse qui n'est pas du JSON valide."
            ) from exc
        except self.requests.exceptions.RequestException as exc:
            raise KanboardAPIError(f"Erreur de connexion à Kanboard : {exc}") from exc

        if not isinstance(body, dict):
            raise KanboardAPIError("Réponse JSON-RPC invalide (objet attendu).")
        if "error" in body:
            error = body["error"]
            raise KanboardAPIError(f"Erreur API pour {method} : {error}")
        if "result" not in body:
            raise KanboardAPIError(f"Réponse JSON-RPC sans résultat pour {method}.")
        if body["result"] is False:
            raise KanboardAPIError(f"L'appel API {method} a échoué.")
        return body["result"]


def normalize_column_name(value: str) -> str:
    without_accents = unicodedata.normalize("NFKD", value)
    return "".join(char for char in without_accents if char.isalnum()).lower()


def get_column_ids(
    client: KanboardClient, project_id: int
) -> dict[str, int]:
    columns = client.call("getColumns", {"project_id": project_id})
    if not isinstance(columns, list):
        raise KanboardAPIError("La liste des colonnes du projet est invalide.")

    resolved: dict[str, int] = {}
    for key, aliases in COLUMN_ALIASES.items():
        named_columns = [
            column
            for column in columns
            if isinstance(column, dict)
            and normalize_column_name(str(column.get("title", "")))
            == normalize_column_name(COLUMN_LABELS[key])
        ]
        # Prefer the explicitly requested label over fallback Kanboard defaults.
        matches = named_columns or [
            column
            for column in columns
            if isinstance(column, dict)
            and normalize_column_name(str(column.get("title", ""))) in aliases
        ]
        if len(matches) != 1:
            names = ", ".join(
                str(column.get("title", "(sans nom)"))
                for column in columns
                if isinstance(column, dict)
            )
            raise KanboardAPIError(
                f"Colonne « {COLUMN_LABELS[key]} » introuvable ou ambiguë. "
                f"Colonnes du projet : {names}. "
                "Adaptez COLUMN_ALIASES au nom exact de vos colonnes."
            )
        try:
            resolved[key] = int(matches[0]["id"])
        except (KeyError, TypeError, ValueError) as exc:
            raise KanboardAPIError(
                f"Identifiant invalide pour la colonne {COLUMN_LABELS[key]}."
            ) from exc
    return resolved


def week_dates(today: date, week_offset: int) -> tuple[str, str]:
    """Return Monday 09:00 and Friday 17:00 for a week relative to today."""
    this_monday = today - timedelta(days=today.weekday())
    monday = this_monday + timedelta(weeks=week_offset)
    friday = monday + timedelta(days=4)
    return (
        datetime.combine(monday, datetime.min.time()).replace(hour=9).strftime(
            "%Y-%m-%d %H:%M"
        ),
        datetime.combine(friday, datetime.min.time()).replace(hour=17).strftime(
            "%Y-%m-%d %H:%M"
        ),
    )


def find_project_id(client: KanboardClient) -> int:
    project = client.call("getProjectByName", {"name": PROJECT_NAME})
    if not isinstance(project, dict) or "id" not in project:
        raise KanboardAPIError(f"Projet « {PROJECT_NAME} » introuvable.")
    try:
        return int(project["id"])
    except (TypeError, ValueError) as exc:
        raise KanboardAPIError("L'identifiant du projet est invalide.") from exc


def get_all_project_tasks(client: KanboardClient, project_id: int) -> list[dict[str, Any]]:
    tasks_by_id: dict[str, dict[str, Any]] = {}
    for status_id in (1, 0):
        tasks = client.call(
            "getAllTasks",
            {"project_id": project_id, "status_id": status_id},
        )
        if not isinstance(tasks, list):
            raise KanboardAPIError(
                f"Liste de tâches invalide (status_id={status_id})."
            )
        for task in tasks:
            if not isinstance(task, dict) or "id" not in task:
                raise KanboardAPIError("Kanboard a renvoyé une tâche invalide.")
            if str(task.get("project_id")) != str(project_id):
                raise KanboardAPIError(
                    "Une tâche renvoyée n'appartient pas au projet ciblé ; "
                    "suppression annulée par sécurité."
                )
            tasks_by_id[str(task["id"])] = task
    return list(tasks_by_id.values())


def show_plan(tasks_to_delete: list[dict[str, Any]], today: date) -> None:
    print(f"Projet : {PROJECT_NAME}")
    print(f"Tâches existantes à supprimer : {len(tasks_to_delete)}")
    print(f"Nouvelles tâches à créer : {len(BACKLOG)}")
    print(f"Sous-tâches à créer : {sum(len(task['subtasks']) for task in BACKLOG)}")
    for task in BACKLOG:
        start, due = week_dates(today, task["week_offset"])
        print(
            f"  [{COLUMN_LABELS[task['column']]}] {task['title']} "
            f"({start} → {due})"
        )


def confirm_reset(assume_yes: bool) -> bool:
    if assume_yes:
        return True
    try:
        answer = input(
            f"Tapez « SUPPRIMER {PROJECT_NAME} » pour continuer : "
        ).strip()
    except EOFError:
        return False
    return answer == f"SUPPRIMER {PROJECT_NAME}"


def reset_and_populate(
    client: KanboardClient,
    project_id: int,
    existing_tasks: list[dict[str, Any]],
    column_ids: dict[str, int],
    today: date,
) -> None:
    # Remove tasks only after the caller has confirmed the exact project name.
    for task in existing_tasks:
        client.call("removeTask", {"task_id": int(task["id"])})
        print(f"Tâche supprimée : {task.get('title', task['id'])}")

    for item in BACKLOG:
        date_started, date_due = week_dates(today, item["week_offset"])
        task_id = client.call(
            "createTask",
            {
                "project_id": project_id,
                "title": item["title"],
                "column_id": column_ids[item["column"]],
                "date_started": date_started,
                "date_due": date_due,
            },
        )
        if isinstance(task_id, bool) or not isinstance(task_id, (int, str)):
            raise KanboardAPIError(
                f"Identifiant invalide renvoyé pour « {item['title']} »."
            )
        print(f"Tâche créée : {item['title']}")

        for subtask_title in item["subtasks"]:
            client.call(
                "createSubtask",
                {"task_id": int(task_id), "title": subtask_title},
            )


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Remplace les tâches du projet Kanboard Cinema par le backlog fourni. "
            "Sans --apply, le script affiche un aperçu sans rien modifier."
        )
    )
    parser.add_argument(
        "--apply",
        action="store_true",
        help="effectuer le nettoyage et créer les tâches (confirmation requise)",
    )
    parser.add_argument(
        "--yes",
        action="store_true",
        help="confirmer sans invite interactive ; nécessite --apply",
    )
    args = parser.parse_args()
    if args.yes and not args.apply:
        parser.error("--yes ne peut être utilisé qu'avec --apply.")
    return args


def main() -> int:
    args = parse_args()
    if not KANBOARD_URL or "example.com" in KANBOARD_URL:
        print(
            "Configurez KANBOARD_URL (dans le script ou l'environnement).",
            file=sys.stderr,
        )
        return 2
    if not API_TOKEN or API_TOKEN == "REMPLACEZ_PAR_VOTRE_TOKEN":
        print(
            "Configurez KANBOARD_API_TOKEN (dans le script ou l'environnement).",
            file=sys.stderr,
        )
        return 2

    client = KanboardClient(KANBOARD_URL, API_USERNAME, API_TOKEN)
    today = date.today()
    try:
        project_id = find_project_id(client)
        existing_tasks = get_all_project_tasks(client, project_id)
        column_ids = get_column_ids(client, project_id)
        show_plan(existing_tasks, today)

        if not args.apply:
            print("\nAucune modification effectuée. Relancez avec --apply.")
            return 0
        if not confirm_reset(args.yes):
            print("Confirmation absente ou incorrecte ; aucune modification effectuée.")
            return 1

        reset_and_populate(
            client, project_id, existing_tasks, column_ids, today
        )
    except KanboardAPIError as exc:
        print(f"Erreur : {exc}", file=sys.stderr)
        return 1

    print("Backlog Cinema injecté avec succès.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
