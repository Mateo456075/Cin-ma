# Cynemma API Client SDK (`cynemma-api-client`)

Bibliothèque cliente Java (SDK officiel sous forme de fichier JAR) permettant aux applications clientes de communiquer de manière typée, robuste et sécurisée avec l'API REST Cynemma (`cynemma-api`).

## Installation

Depuis ce dossier, compilez et installez le JAR dans le dépôt Maven local :

```bash
mvn clean install
```

Le module requiert Java 21 et Maven. Ajoutez ensuite cette dépendance au projet consommateur :

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
| `GET` | `/api/health` | Public | `client.health().checkHealth()` | État de santé du service et de la base MySQL. |
| `GET` | `/api/trailer?film={titre}&year={annee}` | Public | `client.trailers().getTrailer(film, year)` | Recherche de bande-annonce YouTube. |
| `GET` | `/api/internal/tmdb/now-playing` | Bearer JWT (`EMPLOYE`) | `client.tmdb().getNowPlaying()` | Suggestions de films TMDB à l'affiche. |
| `GET` | `/api/internal/tmdb/movies/{tmdbId}` | Bearer JWT (`EMPLOYE`) | `client.tmdb().getMovieDetails(tmdbId)` | Fiche détaillée TMDB d'un film. |
| `GET` | `/api/internal/tmdb/movies/{tmdbId}/poster` | Bearer JWT (`EMPLOYE`) | `client.tmdb().downloadPoster(tmdbId)` | Affiche haute définition en binaire (JPEG/PNG). |
| `POST` | `/api/internal/tmdb/movies/{tmdbId}/trailer-cache` | Bearer JWT (`EMPLOYE`) | `client.tmdb().cacheTrailer(tmdbId, titre)` | Déclenchement de la mise en cache de bande-annonce. |

---

## Guides

- [Utilisation](UTILISATION.md) : création du client et appels disponibles.
- [Authentification et sécurité réseau](AUTHENTIFICATION.md) : fourniture de jetons et règles d'URL.
- [Exceptions](EXCEPTIONS.md) : erreurs HTTP et réseau exposées par le client.
- [Architecture](ARCHITECTURE.md) : organisation du module et flux d'une requête.
