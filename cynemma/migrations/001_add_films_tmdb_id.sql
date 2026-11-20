-- À exécuter une seule fois sur une base Cynemma existante avant de déployer
-- l'import TMDB mis à jour. Les films déjà présents conservent tmdb_id = NULL.
ALTER TABLE films
    ADD COLUMN tmdb_id INT NULL,
    ADD CONSTRAINT uq_films_tmdb_id UNIQUE (tmdb_id);
