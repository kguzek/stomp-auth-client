package uk.guzek.sac.examples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.guzek.sac.AuthType;
import uk.guzek.sac.StompClient;

import java.net.URI;
import java.util.Map;

public class ExampleClient extends StompClient {
    Logger logger = LoggerFactory.getLogger(ExampleClient.class);

    public ExampleClient(URI serverUri, Map<String, String> headers, String host) {
        super(serverUri, headers, host);
    }

    @Override
    public void onStompFrame(String frame, Map<String, String> headers, String body) {
        String logMessage = "Received " + frame + " frame";
        if (body != null && !body.isEmpty() && !body.isBlank()) {
            logMessage += " with body: '" + body + "'";
        }
        logger.debug(logMessage);
    }

    @Override
    public void onClose(int statusCode, String reason, boolean remote) {
        logger.debug("Closed connection " + (remote ? "remote" : "local") + "ly: " + statusCode + " " + reason);
    }

    @Override
    public void onError(Exception e) {
        logger.error("An error occurred: " + e.getMessage());
    }

    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger(ExampleClient.class);
        String serverUri = "ws://localhost:8080/api/v1/staff/stomp";
        // random temporary JWT from my dev environment
        String token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJrZ3V6ZWsiLCJpYXQiOjE3MzIwMjc1MjksImV4cCI6MTczMjA3MDcyOX0.1z_pqhLQA7USpGmBEyjBK6ImHBahGp4-yNn5ovg1Xow";
        // this isn't really necessary, except maybe with strict CORS on your server?
        String host = "test.example.com";
        ExampleClient client = new ExampleClient(URI.create(serverUri), AuthType.jwt(token), host);
        // remember to call `.connect()` after initialising the client
        client.connect();
        client.subscribe("/topic/greetings",
                (Map<String, String> headers, String body) -> logger.debug("Greeted: '" + body + "'")
        );
        client.sendText("test message", "/app/test");
        // sleep to allow replies to propagate, your application will probably be doing
        // other things and won't stop execution immediately after sending out messages
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // soft close, doesn't need additional checks
        client.close();
    }
}
