# Authentification et Sécurité Réseau

## 1. Principe Zéro Clé Privée (Zero Secret in SDK)

Le client JAR `cynemma-api-client` est un artefact partagé et potentiellement distribué. **Aucune clé privée RSA, aucun secret applicatif ou mot de passe n'est stocké dans ce JAR.**

L'application hôte (ex: `cynemma`) est responsable de générer ou détenir ses jetons JWT Bearer signés en RS256, puis de les injecter dynamiquement dans le client via l'interface `TokenProvider` ou par paramètre explicite.

---

## 2. L'interface `TokenProvider`

Pour automatiser l'injection du header HTTP `Authorization: Bearer <token>` sur les requêtes internes, implémentez ou fournissez une lambda `TokenProvider` :

```java
package fr.turgot.client.auth;

@FunctionalInterface
public interface TokenProvider {
    /**
     * Fournit le token JWT Bearer pour la requête en cours.
     */
    String getToken();
}
```

### Exemples d'usage :

1. **Jeton statique (pour scripts ou tests)** :
```java
TokenProvider provider = new StaticTokenProvider("mon-token-jwt");
```

2. **Génération dynamique à la volée (ex: application web principale)** :
```java
CynemmaApiClient client = CynemmaApiClient.builder()
        .baseUrl("http://localhost:8081/cynemma-api")
        .tokenProvider(() -> ApiJwtTokenService.issueEmployeeToken(currentSessionUser.getLogin()))
        .build();
```

---

## 3. Sécurité Réseau et Restriction Loopback (Anti-SSRF)

Le SDK valide strictement chaque URL de base fournie au builder (`HttpRequestExecutor`) :

1. **Schéma HTTPS** : Toujours autorisé pour les communications distantes en production.
2. **Schéma HTTP en clair** : **Strictement interdit** pour les hôtes distants afin d'éviter l'écoute clandestine des tokens JWT.
3. **Exception HTTP de développement** : Autorisée **uniquement** sur les adresses loopback locales sécurisées :
   - `localhost`
   - `127.0.0.1`
   - `::1`
4. **Interdiction des UserInfo** : Toute URL contenant des identifiants intégrés (ex: `http://user:password@...`) est immédiatement rejetée avec une `IllegalArgumentException`.
