# Gestion des Exceptions dans `cynemma-api-client`

Toutes les exceptions générées par le SDK héritent de la classe de base `CynemmaApiException` (`RuntimeException`), ce qui permet une gestion granulaire ou globale selon les besoins de l'application hôte.

## 1. Arborescence des Exceptions

```
java.lang.RuntimeException
└── fr.turgot.client.exception.CynemmaApiException
    ├── CynemmaAuthenticationException (HTTP 401)
    ├── CynemmaForbiddenException (HTTP 403)
    ├── CynemmaNotFoundException (HTTP 404)
    ├── CynemmaValidationException (HTTP 400)
    ├── CynemmaServiceUnavailableException (HTTP 502, 503, 504)
    └── CynemmaNetworkException (Erreurs I/O, timeouts, connexion réseau)
```

---

## 2. Détail des Exceptions et Codes HTTP Associés

| Classe d'Exception | Code HTTP | Description |
|:---|:---|:---|
| `CynemmaValidationException` | `400 Bad Request` | Paramètre invalide, syntaxe JSON rejetée, identifiant négatif. |
| `CynemmaAuthenticationException` | `401 Unauthorized` | Absence d'en-tête `Authorization`, token JWT manquant, invalide ou expiré. |
| `CynemmaForbiddenException` | `403 Forbidden` | Token JWT valide mais rôle insuffisant (ex: non-employé sur `/api/internal/*`). |
| `CynemmaNotFoundException` | `404 Not Found` | Film ou ressource inexistante. |
| `CynemmaServiceUnavailableException` | `502 / 503 / 504` | L'API Cynemma ou le service TMDB externe est temporairement indisponible. |
| `CynemmaNetworkException` | `0` (Pas de réponse) | Timeout de connexion/lecture, socket fermée, hôte inaccessible. |

---

## 3. Exemple de capture d'erreur

```java
import fr.turgot.client.exception.*;

try {
    TmdbMovie movie = client.tmdb().getMovieDetails(9999999);
} catch (CynemmaNotFoundException e) {
    System.err.println("Film non trouvé (404) : " + e.getMessage());
} catch (CynemmaAuthenticationException e) {
    System.err.println("Erreur d'authentification (401) : veuillez renouveler votre jeton.");
} catch (CynemmaForbiddenException e) {
    System.err.println("Accès interdit (403) : rôle employé requis.");
} catch (CynemmaNetworkException e) {
    System.err.println("Problème réseau ou timeout : " + e.getMessage());
} catch (CynemmaApiException e) {
    System.err.println("Erreur API Cynemma [Statut " + e.getStatusCode() + "] : " + e.getMessage());
    System.err.println("Corps brut de réponse : " + e.getErrorBody());
}
```
