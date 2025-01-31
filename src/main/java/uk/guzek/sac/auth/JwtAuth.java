package uk.guzek.sac.auth;

import uk.guzek.sac.AuthType;

import java.util.Map;

public class JwtAuth implements AuthType {
    private final String token;

    /**
     * Initialises a JSON Web Token authentication method.
     *
     * @param token the JWT string
     */
    public JwtAuth(String token) {
        this.token = token;
    }

    @Override
    public Map<String, String> getHeaders() {
        return Map.of("Authorization", "Bearer " + token);
    }
}
