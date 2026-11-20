# Guide d'Installation de `cynemma-api-client`

## 1. Prérequis

- **Java JDK 21** ou supérieur.
- **Apache Maven 3.8+**.
- Dépôt local Maven `~/.m2/repository`.

---

## 2. Compilation et installation dans le dépôt local Maven

Depuis la racine du projet client ou du workspace :

```bash
cd /Volumes/MateoDISQ/Cin-ma/cynemma-api-client
mvn clean install
```

Cette commande compile les sources, exécute la suite de tests unitaires et d'intégration, génère le package binaire JAR dans `cynemma-api-client/target/cynemma-api-client-1.0.0.jar` et l'installe dans le dépôt local :

```
~/.m2/repository/fr/turgot/cynemma-api-client/1.0.0/cynemma-api-client-1.0.0.jar
~/.m2/repository/fr/turgot/cynemma-api-client/1.0.0/cynemma-api-client-1.0.0.pom
```

---

## 3. Ajout de la dépendance dans un projet consommateur

Dans le fichier `pom.xml` de votre application (ex: `cynemma`) :

```xml
<dependencies>
    <!-- SDK Cynemma API Client -->
    <dependency>
        <groupId>fr.turgot</groupId>
        <artifactId>cynemma-api-client</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

Le SDK embarque uniquement la dépendance `org.json:json:20240303` pour la sérialisation, et s'appuie nativement sur le client HTTP standard du JDK (`java.net.http.HttpClient`), garantissant une empreinte minimale sans conflits de bibliothèques tierces.
