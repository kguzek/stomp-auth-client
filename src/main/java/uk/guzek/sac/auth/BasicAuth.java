package uk.guzek.sac.auth;

import uk.guzek.sac.AuthType;

import java.util.Base64;
import java.util.Map;

public class BasicAuth implements AuthType {

    private final String credentials;

    /**
     * Initialises an HTTP Basic authentication method.
     *
     * @param username the username
     * @param password the plaintext password
     */
    public BasicAuth(String username, String password) {
        String concatenated = username + ":" + password;
        credentials = Base64.getEncoder().encodeToString(concatenated.getBytes());
    }

    @Override
    public Map<String, String> getHeaders() {
        return Map.of("Authorization", "Basic " + credentials);
    }
}
