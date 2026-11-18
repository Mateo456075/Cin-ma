package fr.turgot.api.service;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TmdbServiceTest {
    @Test
    public void acceptsConfiguredApiKey() {
        assertEquals("configured-key", TmdbService.requireApiKey("configured-key"));
    }

    @Test
    public void failsWithExplicitConfigurationErrorWhenApiKeyIsMissing() {
        try {
            TmdbService.requireApiKey(" ");
        } catch (TmdbService.TmdbConfigurationException e) {
            assertEquals(
                    "La variable d'environnement TMDB_API_KEY doit être configurée.",
                    e.getMessage());
            return;
        }
        throw new AssertionError("Une clé TMDB absente doit produire une erreur de configuration.");
    }

    @Test
    public void rejectsPlaceholderApiKey() {
        try {
            TmdbService.requireApiKey("METTRE_TA_CLE_TMDB_ICI");
        } catch (TmdbService.TmdbConfigurationException e) {
            assertEquals(
                    "Remplace le placeholder de TMDB_API_KEY par une vraie clé TMDB.",
                    e.getMessage());
            return;
        }
        throw new AssertionError("Le placeholder ne doit pas être accepté comme clé TMDB.");
    }
}
