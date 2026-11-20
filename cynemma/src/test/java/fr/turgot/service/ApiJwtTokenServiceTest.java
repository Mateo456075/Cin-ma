package fr.turgot.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class ApiJwtTokenServiceTest {
    private KeyPair keyPair;

    @Before
    public void createKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
    }

    @Test
    public void issuesShortLivedEmployeeJwtSignedWithTheConfiguredKey() throws Exception {
        String token = ApiJwtTokenService.issueEmployeeToken("employee-login", keyPair.getPrivate());
        String[] parts = token.split("\\.");

        JSONObject claims = new JSONObject(new String(
                Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8));
        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(keyPair.getPublic());
        verifier.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));

        assertEquals("cynemma-main", claims.getString("iss"));
        assertEquals("cynemma-api", claims.getString("aud"));
        assertEquals("employee-login", claims.getString("sub"));
        assertEquals("EMPLOYE", claims.getString("role"));
        assertTrue(claims.getLong("exp") - claims.getLong("iat") == 300);
        assertTrue(verifier.verify(Base64.getUrlDecoder().decode(parts[2])));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingEmployeeLogin() {
        ApiJwtTokenService.issueEmployeeToken(" ", keyPair.getPrivate());
    }
}
