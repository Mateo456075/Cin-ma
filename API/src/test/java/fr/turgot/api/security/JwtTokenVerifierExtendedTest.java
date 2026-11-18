package fr.turgot.api.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtTokenVerifierExtendedTest {
    private KeyPair keyPair;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
    }

    @Test
    void rejectsWrongIssuerAudienceAndTooOldToken() throws Exception {
        long now = System.currentTimeMillis() / 1000;
        assertEquals(JwtTokenVerifier.AuthorizationResult.UNAUTHORIZED,
                JwtTokenVerifier.verifyEmployeeToken(
                        signedToken("EMPLOYE", "wrong-issuer", "cynemma-api", now),
                        keyPair.getPublic()));
        assertEquals(JwtTokenVerifier.AuthorizationResult.UNAUTHORIZED,
                JwtTokenVerifier.verifyEmployeeToken(
                        signedToken("EMPLOYE", "cynemma-main", "wrong-audience", now),
                        keyPair.getPublic()));
        assertEquals(JwtTokenVerifier.AuthorizationResult.UNAUTHORIZED,
                JwtTokenVerifier.verifyEmployeeToken(
                        signedToken("EMPLOYE", "cynemma-main", "cynemma-api", now - 600),
                        keyPair.getPublic()));
    }

    @Test
    void rejectsMalformedAndWrongAlgorithmTokens() throws Exception {
        assertEquals(JwtTokenVerifier.AuthorizationResult.UNAUTHORIZED,
                JwtTokenVerifier.verifyEmployeeToken("not.a.token", keyPair.getPublic()));
        assertEquals(JwtTokenVerifier.AuthorizationResult.UNAUTHORIZED,
                JwtTokenVerifier.verifyEmployeeToken(
                        "eyJhbGciOiJub25lIn0.e30.signature", keyPair.getPublic()));
    }

    private String signedToken(String role, String issuer, String audience, long issuedAt)
            throws Exception {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String header = encoder.encodeToString(
                "{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = encoder.encodeToString(new JSONObject()
                .put("iss", issuer)
                .put("aud", audience)
                .put("sub", "employee")
                .put("role", role)
                .put("iat", issuedAt)
                .put("exp", issuedAt + 300)
                .put("jti", "test")
                .toString().getBytes(StandardCharsets.UTF_8));
        String unsigned = header + "." + payload;
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(keyPair.getPrivate());
        signature.update(unsigned.getBytes(StandardCharsets.US_ASCII));
        return unsigned + "." + encoder.encodeToString(signature.sign());
    }
}
