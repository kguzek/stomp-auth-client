package uk.guzek.sac;

import java.util.Map;

public class AuthType {
    /**
     * Obtains the appropriate headers for authentication made using JSON Web Tokens (JWTs).
     * @param token the JWT string
     * @return a header map containing the formatted `Authorization` header
     */
    public static Map<String, String> jwt(String token) {
        return Map.of("Authorization", "Bearer " + token);
    }
}
