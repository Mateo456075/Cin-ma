package fr.turgot.api.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.json.JSONObject;

import jakarta.servlet.http.HttpServletRequest;

public final class JwtTokenVerifier {
    private static final String PUBLIC_KEY_ENVIRONMENT_VARIABLE = "CYNEMMA_JWT_PUBLIC_KEY";
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private JwtTokenVerifier() {
    }

    public static AuthorizationResult authorizeEmployee(HttpServletRequest request) {
        var publicKey = readPublicKey();
        if (publicKey == null) {
            return AuthorizationResult.NOT_CONFIGURED;
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return AuthorizationResult.UNAUTHORIZED;
        }
        return verifyEmployeeToken(
                authorization.substring("Bearer ".length()), publicKey);
    }

    static AuthorizationResult verifyEmployeeToken(
            String token, java.security.PublicKey publicKey) {
        if (token.length() > 8192) {
            return AuthorizationResult.UNAUTHORIZED;
        }
        String[] parts = token.split("\\.", -1);
        if (parts.length != 3 || parts[0].length() > 2048
                || parts[1].length() > 4096 || parts[2].length() > 2048) {
            return AuthorizationResult.UNAUTHORIZED;
        }

        try {
            JSONObject header = new JSONObject(new String(
                    URL_DECODER.decode(parts[0]), StandardCharsets.UTF_8));
            if (!"RS256".equals(header.optString("alg"))) {
                return AuthorizationResult.UNAUTHORIZED;
            }

            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(publicKey);
            verifier.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
            if (!verifier.verify(URL_DECODER.decode(parts[2]))) {
                return AuthorizationResult.UNAUTHORIZED;
            }

            JSONObject claims = new JSONObject(new String(
                    URL_DECODER.decode(parts[1]), StandardCharsets.UTF_8));
            long now = System.currentTimeMillis() / 1000;
            long issuedAt = claims.getLong("iat");
            long expiresAt = claims.getLong("exp");
            if (!"cynemma-main".equals(claims.optString("iss"))
                    || !"cynemma-api".equals(claims.optString("aud"))
                    || claims.optString("sub").isBlank()
                    || issuedAt > now + 30
                    || expiresAt <= now
                    || expiresAt <= issuedAt
                    || expiresAt - issuedAt > 300) {
                return AuthorizationResult.UNAUTHORIZED;
            }
            return "EMPLOYE".equals(claims.optString("role"))
                    ? AuthorizationResult.AUTHORIZED
                    : AuthorizationResult.FORBIDDEN;
        } catch (GeneralSecurityException | RuntimeException e) {
            return AuthorizationResult.UNAUTHORIZED;
        }
    }

    private static java.security.PublicKey readPublicKey() {
        String encodedKey = System.getenv(PUBLIC_KEY_ENVIRONMENT_VARIABLE);
        if (encodedKey == null || encodedKey.isBlank()) {
            return null;
        }
        try {
            byte[] keyBytes = Base64.getDecoder().decode(encodedKey);
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyBytes));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            return null;
        }
    }

    public enum AuthorizationResult {
        AUTHORIZED,
        UNAUTHORIZED,
        FORBIDDEN,
        NOT_CONFIGURED
    }
}
