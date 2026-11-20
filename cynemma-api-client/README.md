# Cynemma API Client SDK (`cynemma-api-client`)

Bibliothèque cliente Java (SDK officiel sous forme de fichier JAR) permettant aux applications clientes de communiquer de manière typée, robuste et sécurisée avec l'API REST Cynemma (`cynemma-api`).

## Coordonnées Maven

```xml
<dependency>
    <groupId>fr.turgot</groupId>
    <artifactId>cynemma-api-client</artifactId>
    <version>1.0.0</version>
</dependency>
```

- **Groupe** : `fr.turgot`
- **Artefact** : `cynemma-api-client`
- **Version** : `1.0.0`
- **Packaging** : `jar`
- **Version Java cible** : Java 21+

---

## Fonctionnalités couvertes

Le SDK encapsule et type l'ensemble des endpoints réels de l'API Cynemma :

| Méthode HTTP | Chemin Endpoint | Authentification | Service SDK | Description |
|:---|:---|:---|:---|:---|
| `GET` | `/api/health` | Public | `client.health().checkHealth()` | État de santé du service et de la base MariaDB. |
| `GET` | `/api/trailer?film={titre}&year={annee}` | Public | `client.trailers().getTrailer(film, year)` | Recherche de bande-annonce YouTube. |
| `GET` | `/api/internal/tmdb/now-playing` | Bearer JWT (`EMPLOYE`) | `client.tmdb().getNowPlaying()` | Suggestions de films TMDB à l'affiche. |
| `GET` | `/api/internal/tmdb/movies/{tmdbId}` | Bearer JWT (`EMPLOYE`) | `client.tmdb().getMovieDetails(tmdbId)` | Fiche détaillée TMDB d'un film. |
| `GET` | `/api/internal/tmdb/movies/{tmdbId}/poster` | Bearer JWT (`EMPLOYE`) | `client.tmdb().downloadPoster(tmdbId)` | Affiche haute définition en binaire (JPEG/PNG). |
| `POST` | `/api/internal/tmdb/movies/{tmdbId}/trailer-cache` | Bearer JWT (`EMPLOYE`) | `client.tmdb().cacheTrailer(tmdbId, titre)` | Déclenchement de la mise en cache de bande-annonce. |

---

## Documentation détaillée

- [Guide d'installation](file:///Volumes/MateoDISQ/Cin-ma/cynemma-api-client/INSTALLATION.md) : Prérequis, dépendances et build Maven.
- [Guide d'utilisation](file:///Volumes/MateoDISQ/Cin-ma/cynemma-api-client/UTILISATION.md) : Instanciation, configuration du builder, exemples d'appels.
- [Authentification et Sécurité](file:///Volumes/MateoDISQ/Cin-ma/cynemma-api-client/AUTHENTIFICATION.md) : Gestion des tokens JWT RS256 et politique de sécurité réseau (loopback / HTTPS).
- [Gestion des Exceptions](file:///Volumes/MateoDISQ/Cin-ma/cynemma-api-client/EXCEPTIONS.md) : Hiérarchie typée des erreurs HTTP et réseau.
- [Architecture technique](file:///Volumes/MateoDISQ/Cin-ma/cynemma-api-client/ARCHITECTURE.md) : Conception logicielle, découplage et structure interne du module.
