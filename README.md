# STOMP Auth Client

## Description

SAC is essentially a STOMP wrapper to [Java-WebSocket](https://github.com/TooTallNate/Java-WebSocket). I created it since I use HTTP-based authentication on my Spring Boot server, and STOMP messaging is very easy to integrate into that existing workflow.
However, most STOMP clients use a custom authentication method done internally, and require the HTTP Upgrade route to be unprotected on the server. This is why I decided to create my own library which allows the authentication to be performed on the request level.

For more details on the issue addressed by this solution, refer to this discussion on StackOverflow:

- https://stackoverflow.com/questions/45405332/websocket-authentication-and-authorization-in-spring

This approach isn't needed when integrating websockets into a Spring-based client, but it is the only currently available option when creating non-Spring Java applications.

## Getting Started

### Installation

[WIP] 
To be made available on the Maven Central Repository.

### Logging

Please refer to the Java-WebSocket README regarding logging, as the same applies here.

https://github.com/TooTallNate/Java-WebSocket#logging

## Usage

{install/add to pom}

The API is very similar to Java-Websocket's: you need to create your own implementation of the `StompClient` abstract class, then operate on an instance of that class.

Assuming we have such a class called `ExampleClient`:

```java
ExampleClient client = new ExampleClient(URI.create(serverUri), AuthType.jwt(token), host);
// remember to call `.connect()` after initialising the client
client.connect();
client.subscribe("/topic/greetings",
        (Map<String, String> headers, String body) -> logger.debug("Greeted: '" + body + "'")
);
client.sendText("test message", "/app/test");
```

Below is an example of code that could be used to create a web server in Spring which would correctly handle the above client invocation.

```java
// configuration class
@Configuration
@RequiredArgsConstructor
@EnableWebSocketMessageBroker
public class ApplicationConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        registry.addEndpoint("/api/v1/staff/stomp").setAllowedOrigins("*");
    }

    @Override
    public void configureMessageBroker(@NonNull MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }
}

// controller class
@Controller
public class WebsocketController {
    @MessageMapping("/test")
    @SendTo("/topic/greetings")
    public String greeting(String message) {
        System.out.println("Received message: '" + message + "'");
        return "Hello, " + message + "!";
    }
}
```

For a more complete client implementation example, refer to [src/main/java/uk/guzek/sac/examples/ExampleClient.java](https://github.com/kguzek/stomp-auth-client/blob/main/src/main/java/uk/guzek/sac/examples/ExampleClient.java).

###### Thank you for reading!
