-- Schéma MySQL/InnoDB pour Cynemma.
-- Les contraintes CHECK sont appliquées à partir de MySQL 8.0.16.
-- Les versions MySQL antérieures acceptent ces contraintes mais les ignorent :
-- les mêmes règles doivent alors être validées par l'application.

CREATE TABLE utilisateurs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(70) NULL,
    email VARCHAR(254) NOT NULL,
    mot_de_passe VARCHAR(255) NOT NULL,
    login VARCHAR(50) NOT NULL,
    type_utilisateur VARCHAR(20) NOT NULL DEFAULT 'CLIENT',
    CONSTRAINT uq_utilisateurs_email UNIQUE (email),
    CONSTRAINT uq_utilisateurs_login UNIQUE (login),
    CONSTRAINT chk_utilisateurs_type
        CHECK (type_utilisateur IN ('CLIENT', 'EMPLOYE', 'ADMINISTRATEUR'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE cartes_fidelite (
    id INT AUTO_INCREMENT PRIMARY KEY,
    numero_carte VARCHAR(50) NOT NULL,
    points INT NOT NULL DEFAULT 0,
    abonne_id INT NOT NULL,
    CONSTRAINT uq_cartes_numero UNIQUE (numero_carte),
    CONSTRAINT uq_cartes_abonne UNIQUE (abonne_id),
    CONSTRAINT chk_cartes_points CHECK (points >= 0),
    CONSTRAINT fk_cartes_abonne
        FOREIGN KEY (abonne_id) REFERENCES utilisateurs(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE configuration_salles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    espacement_rangees DOUBLE NULL,
    largeur_allee DOUBLE NULL,
    nombre_balcons INT NOT NULL DEFAULT 0,
    disposition VARCHAR(30) NOT NULL,
    CONSTRAINT chk_configuration_balcons CHECK (nombre_balcons >= 0),
    CONSTRAINT chk_configuration_disposition
        CHECK (disposition IN ('IMAX', 'SCREEN_X', 'RECTANGLE', 'ARC_DE_CERCLE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE salles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(255) NOT NULL,
    capacite INT NOT NULL DEFAULT 0,
    configuration_id INT NOT NULL,
    type_salle VARCHAR(30) NOT NULL,
    CONSTRAINT uq_salles_configuration UNIQUE (configuration_id),
    CONSTRAINT chk_salles_capacite CHECK (capacite >= 0),
    CONSTRAINT chk_salles_type
        CHECK (type_salle IN ('QUATRE_DX', 'CANAPE', 'LIT', 'CONFOR_PLUS', 'STANDARD')),
    CONSTRAINT fk_salles_configuration
        FOREIGN KEY (configuration_id) REFERENCES configuration_salles(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE rangees (
    id INT AUTO_INCREMENT PRIMARY KEY,
    salle_id INT NOT NULL,
    nom_rangee VARCHAR(20) NOT NULL,
    nombre_sieges INT NOT NULL,
    elevation_y DOUBLE NULL,
    INDEX idx_rangees_salle_id (salle_id),
    CONSTRAINT chk_rangees_nombre_sieges CHECK (nombre_sieges >= 0),
    CONSTRAINT fk_rangees_salle
        FOREIGN KEY (salle_id) REFERENCES salles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sieges (
    id INT AUTO_INCREMENT PRIMARY KEY,
    rangee_id INT NOT NULL,
    numero INT NOT NULL,
    coordonnee_x DOUBLE NULL,
    coordonnee_z DOUBLE NULL,
    angle_rotation DOUBLE NULL,
    CONSTRAINT uq_sieges_rangee_numero UNIQUE (rangee_id, numero),
    CONSTRAINT chk_sieges_numero CHECK (numero > 0),
    CONSTRAINT fk_sieges_rangee
        FOREIGN KEY (rangee_id) REFERENCES rangees(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE films (
    id INT AUTO_INCREMENT PRIMARY KEY,
    titre VARCHAR(255) NOT NULL,
    duree_minutes INT NOT NULL,
    realisateur VARCHAR(255) NOT NULL,
    annee INT NOT NULL,
    genre VARCHAR(100) NOT NULL,
    affiche VARCHAR(255) NOT NULL,
    CONSTRAINT chk_films_duree CHECK (duree_minutes > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE seances (
    id INT AUTO_INCREMENT PRIMARY KEY,
    film_id INT NOT NULL,
    salle_id INT NOT NULL,
    type_seance VARCHAR(30) NOT NULL DEFAULT 'STANDARD',
    date_heure_debut DATETIME NOT NULL,
    date_heure_fin DATETIME NULL,
    INDEX idx_seances_film_debut (film_id, date_heure_debut),
    INDEX idx_seances_salle_debut (salle_id, date_heure_debut),
    CONSTRAINT chk_seances_type
        CHECK (type_seance IN ('TROIS_D', 'VIP', 'STANDARD')),
    CONSTRAINT chk_seances_horaires
        CHECK (date_heure_fin IS NULL OR date_heure_fin > date_heure_debut),
    CONSTRAINT fk_seances_film
        FOREIGN KEY (film_id) REFERENCES films(id),
    CONSTRAINT fk_seances_salle
        FOREIGN KEY (salle_id) REFERENCES salles(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE reservations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    client_id INT NOT NULL,
    date_reservation DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    montant_total DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    INDEX idx_reservations_client_id (client_id),
    CONSTRAINT chk_reservations_montant CHECK (montant_total >= 0),
    CONSTRAINT fk_reservations_client
        FOREIGN KEY (client_id) REFERENCES utilisateurs(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE billets (
    id INT AUTO_INCREMENT PRIMARY KEY,
    reservation_id INT NOT NULL,
    seance_id INT NOT NULL,
    siege_id INT NOT NULL,
    tarif_applique DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    UNIQUE KEY uq_billets_seance_siege (seance_id, siege_id),
    INDEX idx_billets_reservation_id (reservation_id),
    CONSTRAINT chk_billets_tarif CHECK (tarif_applique >= 0),
    CONSTRAINT fk_billets_reservation
        FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE CASCADE,
    CONSTRAINT fk_billets_seance
        FOREIGN KEY (seance_id) REFERENCES seances(id),
    CONSTRAINT fk_billets_siege
        FOREIGN KEY (siege_id) REFERENCES sieges(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
