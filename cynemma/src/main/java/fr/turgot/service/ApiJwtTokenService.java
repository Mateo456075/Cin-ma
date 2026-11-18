package fr.turgot.service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

import org.json.JSONObject;

public final class ApiJwtTokenService {
    private static final String PRIVATE_KEY_ENVIRONMENT_VARIABLE = "CYNEMMA_JWT_PRIVATE_KEY";
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private ApiJwtTokenService() {
    }

    public static String issueEmployeeToken(String employeeLogin) {
        if (employeeLogin == null || employeeLogin.isBlank()) {
            throw new IllegalArgumentException("L'identifiant de l'employé est obligatoire.");
        }
        return issueEmployeeToken(employeeLogin, readPrivateKey());
    }

    static String issueEmployeeToken(String employeeLogin, PrivateKey privateKey) {
        if (employeeLogin == null || employeeLogin.isBlank()) {
            throw new IllegalArgumentException("L'identifiant de l'employé est obligatoire.");
        }
        try {
            JSONObject header = new JSONObject()
                    .put("alg", "RS256")
                    .put("typ", "JWT");
            long issuedAt = System.currentTimeMillis() / 1000;
            JSONObject payload = new JSONObject()
                    .put("iss", "cynemma-main")
                    .put("aud", "cynemma-api")
                    .put("sub", employeeLogin)
                    .put("role", "EMPLOYE")
                    .put("iat", issuedAt)
                    .put("exp", issuedAt + 300)
                    .put("jti", UUID.randomUUID().toString());

            String unsignedToken = encode(header.toString()) + "." + encode(payload.toString());
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(unsignedToken.getBytes(StandardCharsets.US_ASCII));
            return unsignedToken + "." + URL_ENCODER.encodeToString(signature.sign());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Impossible de signer le jeton d'accès à l'API.", e);
        }
    }

    private static PrivateKey readPrivateKey() {
        String encodedKey = System.getenv(PRIVATE_KEY_ENVIRONMENT_VARIABLE);
        if (encodedKey == null || encodedKey.isBlank()) {
            throw new IllegalStateException(
                    PRIVATE_KEY_ENVIRONMENT_VARIABLE + " doit être configurée.");
        }
        try {
            return KeyFactory.getInstance("RSA").generatePrivate(
                    new PKCS8EncodedKeySpec(Base64.getDecoder().decode(encodedKey)));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    PRIVATE_KEY_ENVIRONMENT_VARIABLE + " doit contenir une clé PKCS#8 encodée en Base64.",
                    e);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(
                    PRIVATE_KEY_ENVIRONMENT_VARIABLE + " doit contenir une clé RSA PKCS#8 valide.", e);
        }
    }

    private static String encode(String value) {
        return URL_ENCODER.encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
