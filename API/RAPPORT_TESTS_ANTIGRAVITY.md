# Rapport de campagne de tests — Antigravity

Date: 18 novembre 2026

## A. Résumé global

| Indicateur | Valeur |
|---|---:|
| Tests existants avant campagne | 37 |
| Tests créés | 9 |
| Tests modifiés | 1 |
| Tests exécutés | 46 |
| Tests réussis | 46 |
| Tests échoués | 0 |
| Tests non exécutés par dépendance externe | MySQL, Tomcat, TMDB réel, Playwright |

## B. Résultats par projet

### Cynemma API

- Baseline: 14 tests, tous réussis.
- Campagne finale: 20 tests, tous réussis.
- `mvn clean verify`: BUILD SUCCESS, code 0.
- Couverture instruction: 20,69%.
- Couverture lignes: 19,42%.
- Couverture branches: 18,18%.
- Rapport: `API/target/site/jacoco/index.html`

### Cynemma

- Baseline: 23 tests, tous réussis.
- Campagne finale: 26 tests, tous réussis.
- `mvn clean verify`: BUILD SUCCESS, code 0.
- Couverture instruction: 11,89%.
- Couverture lignes: 10,03%.
- Couverture branches: 12,43%.
- Rapport: `cynemma/target/site/jacoco/index.html`

## C. Résultats par catégorie

| Catégorie | Résultat |
|---|---|
| Tests unitaires JUnit/Mockito | Réalisés sur les classes critiques disponibles |
| Tests d'intégration | Non exécutés: Docker/MySQL/Tomcat absents |
| Tests API HTTP | Tests Servlet en mémoire exécutés; serveur HTTP réel non démarré |
| Tests de sécurité | JWT, rôles, paramètres, PageServlet, billetterie partiellement exécutés |
| Tests de non-régression | Année, coordonnées 3D, balcons, liste blanche et JNDI |
| Tests de génération 3D | 3 tests exécutés pour les quatre dispositions |
| Tests OpenAPI | Validation statique précédente non relancée par un outil externe |

## D. Anomalies découvertes

### 1. Année de sortie validée seulement dans le service

- Fichier: `API/src/main/java/fr/turgot/api/controller/TrailerServlet.java`
- Gravité: Moyenne.
- Correction: validation `1888..9999` ajoutée dans `TrailerServlet.parseOptionalYear`.
- Test: `TrailerServletParametersTest`.

### 2. Coordonnées 3D jamais calculées

- Fichier: `cynemma/src/main/java/fr/turgot/servlet/admin/CreatSiegeServlet.java`
- Fichiers ajoutés: `cynemma/src/main/java/fr/turgot/utils/SeatLayoutCalculator.java`
- Gravité: Élevée.
- Correction: recalcul déterministe des rangées et sièges après création.
- Test: `SeatLayoutCalculatorTest`.

### 3. Docker indisponible

- Fichier concerné: infrastructure uniquement.
- Gravité: Moyenne pour l'intégration.
- Correction: aucune; les tests unitaires restent exécutables.
- Statut: bloqué par la dépendance externe.

### 4. Playwright Chromium indisponible

- Gravité: Moyenne pour le rendu JSP.
- Correction: aucune dans l'application.
- Statut: téléchargement Chromium stoppé après timeout réseau.

## E. Commandes d'exécution

```bash
cd /Volumes/MateoDISQ/Cin-ma/API
mvn clean test
mvn clean verify

cd /Volumes/MateoDISQ/Cin-ma/cynemma
mvn clean test
mvn clean verify
```

Rapports Surefire:

- `API/target/surefire-reports/`
- `cynemma/target/surefire-reports/`

Rapports JaCoCo:

- `API/target/site/jacoco/index.html`
- `cynemma/target/site/jacoco/index.html`

## F. Tests manuels restants

- Déploiement Tomcat des deux WAR.
- MySQL réel et DataSource JNDI.
- Docker/MySQL/Testcontainers.
- Clé TMDB réelle et appels réseau.
- SMTP réel.
- Parcours JSP dans un navigateur.
- Vérification visuelle Three.js.
- Réservation simultanée sur une base réelle.

## G. Verdict

**Partiellement validés**.

Les deux modules compilent et leurs campagnes unitaires passent. Les corrections testées
pour la validation des années et les coordonnées 3D sont vérifiées. Les tests dépendant
de Docker, MySQL, Tomcat, TMDB réel, SMTP ou Chromium n'ont pas été exécutés et ne sont pas
présentés comme validés.

## Verdict environnement

```text
environment:
  docker: UNAVAILABLE — docker info indisponible
  node: AVAILABLE — v24.14.0
  playwright: UNAVAILABLE — téléchargement Chromium interrompu par timeout réseau
  infra-tier: FALLBACK(embedded/in-memory) — Docker absent
  browser-tier: FALLBACK(framework-native HTTP) — Chromium indisponible
startup: UNVERIFIED — WAR Jakarta non démarrés faute de Tomcat actif
integration: UNVERIFIED — exit_code: n/a, scope Maven unitonly, gap: MySQL/Tomcat
e2e: UNVERIFIED — exit_code: n/a, gap: navigateur et serveur HTTP
overall: NEEDS_SIGNOFF — campagnes unitaires vertes, intégrations externes en attente
```
