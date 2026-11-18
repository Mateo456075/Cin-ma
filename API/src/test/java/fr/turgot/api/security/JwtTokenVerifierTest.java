package fr.turgot.api.security;

import static org.junit.Assert.assertEquals;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class JwtTokenVerifierTest {
    private KeyPair keyPair;

    @Before
    public void createKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
    }

    @Test
    public void acceptsCurrentEmployeeToken() throws Exception {
        String token = token("EMPLOYE", System.currentTimeMillis() / 1000);

        assertEquals(
                JwtTokenVerifier.AuthorizationResult.AUTHORIZED,
                JwtTokenVerifier.verifyEmployeeToken(token, keyPair.getPublic()));
    }

    @Test
    public void rejectsExpiredEmployeeToken() throws Exception {
        String token = token("EMPLOYE", System.currentTimeMillis() / 1000 - 600);

        assertEquals(
                JwtTokenVerifier.AuthorizationResult.UNAUTHORIZED,
                JwtTokenVerifier.verifyEmployeeToken(token, keyPair.getPublic()));
    }

    @Test
    public void forbidsValidTokenWithNonEmployeeRole() throws Exception {
        String token = token("CLIENT", System.currentTimeMillis() / 1000);

        assertEquals(
                JwtTokenVerifier.AuthorizationResult.FORBIDDEN,
                JwtTokenVerifier.verifyEmployeeToken(token, keyPair.getPublic()));
    }

    @Test
    public void rejectsTamperedToken() throws Exception {
        String[] parts = token("EMPLOYE", System.currentTimeMillis() / 1000).split("\\.");
        parts[1] = Base64.getUrlEncoder().withoutPadding().encodeToString(
                new JSONObject().put("role", "EMPLOYE").put("sub", "attacker")
                        .put("iss", "cynemma-main").put("aud", "cynemma-api")
                        .put("iat", System.currentTimeMillis() / 1000)
                        .put("exp", System.currentTimeMillis() / 1000 + 300)
                        .toString().getBytes(StandardCharsets.UTF_8));

        assertEquals(
                JwtTokenVerifier.AuthorizationResult.UNAUTHORIZED,
                JwtTokenVerifier.verifyEmployeeToken(
                        parts[0] + "." + parts[1] + "." + parts[2], keyPair.getPublic()));
    }

    private String token(String role, long issuedAt) throws Exception {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String header = encoder.encodeToString(
                "{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = encoder.encodeToString(new JSONObject()
                .put("iss", "cynemma-main")
                .put("aud", "cynemma-api")
                .put("sub", "employe-test")
                .put("role", role)
                .put("iat", issuedAt)
                .put("exp", issuedAt + 300)
                .put("jti", "test-token")
                .toString().getBytes(StandardCharsets.UTF_8));
        String unsigned = header + "." + payload;
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(keyPair.getPrivate());
        signer.update(unsigned.getBytes(StandardCharsets.US_ASCII));
        return unsigned + "." + encoder.encodeToString(signer.sign());
    }
}
