/**
 * STOMP Auth Client - A simple STOMP client for Java with authentication support
 * Copyright © 2024 by Konrad Guzek <konrad@guzek.uk>
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package uk.guzek.sac;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.*;

public abstract class StompClient extends WebSocketClient {
    private final String host;
    private final int timeout; // deciseconds; default 10 seconds
    private final Map<Integer, Runnable> receiptHandlers = new HashMap<>();
    private final Map<String, SubscriptionHandler> subscriptionHandlers = new HashMap<>();
    private final Collection<Runnable> messageQueue = new ArrayList<>();
    private int receiptId = 0;
    private int subscriptionId = 0;
    private boolean connected = false;
    Logger logger = LoggerFactory.getLogger(StompClient.class);

    /**
     * Initialises the client.
     *
     * @param serverUri the uri of the server
     * @param host      the host to use to send the CONNECT STOMP frame
     */
    public StompClient(URI serverUri, String host) {
        this(serverUri, Map.of(), host);
    }

    /**
     * Initialises the client.
     *
     * @param serverUri the URI of the STOMP server
     * @param headers   optional key-value header pairs to use during the initial
     *                  HTTP Upgrade request
     * @param host      the host to use to send the CONNECT STOMP frame
     */
    public StompClient(URI serverUri, Map<String, String> headers, String host) {
        this(serverUri, headers, host, 10);
    }

    /**
     * Initialises the client.
     *
     * @param serverUri the URI of the STOMP server
     * @param authType  optional authentication method from `uk.guzek.sac.auth.*` to use during the initial
     *                  HTTP Upgrade request
     * @param host      the host to use to send the CONNECT STOMP frame
     */
    public StompClient(URI serverUri, AuthType authType, String host) {
        this(serverUri, authType, host, 10);
    }

    /**
     * Initialises the client.
     *
     * @param serverUri the URI of the STOMP server
     * @param authType  optional authentication method from `uk.guzek.sac.auth.*` to use during the initial
     *                  HTTP Upgrade request
     * @param host      the host to use to send the CONNECT STOMP frame
     * @param timeout   optional length of time to allow for lost messages to be
     *                  sent when the client is closed, in
     *                  seconds; defaults to 10
     */
    public StompClient(URI serverUri, AuthType authType, String host, int timeout) {
        this(serverUri, authType.getHeaders(), host, timeout);
    }

    /**
     * Initialises the client.
     *
     * @param serverUri the URI of the STOMP server
     * @param headers   optional key-value header pairs to use during the initial
     *                  HTTP Upgrade request
     * @param host      the host to use to send the CONNECT STOMP frame
     * @param timeout   optional length of time to allow for lost messages to be
     *                  sent when the client is closed, in
     *                  seconds; defaults to 10
     */
    public StompClient(URI serverUri, Map<String, String> headers, String host, int timeout) {
        super(serverUri, headers);
        this.host = host;
        this.timeout = timeout * 10;
    }

    private void executeMessageQueue() {
        if (!connected) return;
        for (Runnable runnable : messageQueue) {
            runnable.run();
        }
        messageQueue.clear();
    }

    /**
     * Send a customisable generic STOMP frame.
     * <a href="https://github.com/stomp/stomp-spec/blob/master/src/stomp-specification-1.2.md">STOMP specifications</a>
     *
     * @param command the frame command
     * @param headers a map of header key-value pairs
     */
    private void sendStompFrame(String command, Map<String, Object> headers) {
        sendStompFrame(command, headers, "");
    }

    /**
     * Send a customisable generic STOMP frame.
     * <a href="https://github.com/stomp/stomp-spec/blob/master/src/stomp-specification-1.2.md">STOMP specifications</a>
     *
     * @param command the frame command
     * @param headers a map of header key-value pairs
     * @param body    optional payload string
     */
    public void sendStompFrame(String command, Map<String, Object> headers, String body) {
        StringBuilder messageBuilder = new StringBuilder(command.toUpperCase() + "\n");
        for (Map.Entry<String, Object> header : headers.entrySet()) {
            messageBuilder.append(header.getKey()).append(":").append(header.getValue().toString()).append("\n");
        }
        String message = messageBuilder.append("\n").append(body).append("\0").toString();

        Runnable runnable = () -> {
            logger.trace(">>>{}", message);
            this.send(message);
        };

        if (connected || command.equals("CONNECT")) {
            runnable.run();
        } else {
            messageQueue.add(runnable);
        }
    }

    private void sendConnectFrame() {
        sendStompFrame("CONNECT", Map.of("host", host, "accept-version", "1.2"));
    }

    private int sendDisconnectFrame() {
        sendStompFrame("DISCONNECT", Map.of("receipt", receiptId));
        return receiptId++;
    }

    /**
     * Send a SEND frame which is not JSON or plaintext.
     *
     * @param message     the payload to send
     * @param destination the path to send the message to
     * @param contentType the MIME content type of the payload
     */
    public void sendSendFrame(String message, String destination, String contentType) {
        sendStompFrame("SEND", Map.of("destination", destination, "content-type", contentType), message);
    }

    /**
     * Send a plaintext SEND frame.
     *
     * @param message     the plaintext payload to send
     * @param destination the path to send the message to
     */
    public void sendText(String message, String destination) {
        sendSendFrame(message, destination, "text/plain");
    }

    /**
     * Send a JSON SEND frame.
     *
     * @param object      the object payload to be serialised as JSON and sent
     * @param destination the path to send the message to
     * @throws JsonProcessingException passed on from Jackson's `writeValueAsString`
     */
    public void sendJson(Object object, String destination) throws JsonProcessingException {
        String json = new ObjectMapper().writer().writeValueAsString(object);
        sendSendFrame(json, destination, "application/json");
    }

    /**
     * Calls `handler` whenever the server sends MESSAGE frames to `destination`.
     *
     * @param destination the path of the resource to subscribe to
     * @param handler     a biconsumable which takes a string-string header map and a body string`
     * @return the id of the subscription (incremental)
     */
    public int subscribe(String destination, SubscriptionHandler handler) {
        sendStompFrame("SUBSCRIBE", Map.of("destination", destination, "id", subscriptionId));
        subscriptionHandlers.put(destination, handler);
        return subscriptionId++;
    }

    /**
     * Removes the subscription with the given id from the given destination.
     * @param destination the path of the resource that was subscribed to
     * @param subscriptionId the integer returned from the corresponding call to {@link #subscribe}
     */
    public void unsubscribe(String destination, int subscriptionId) {
        sendStompFrame("UNSUBSCRIBE", Map.of("id", subscriptionId));
        subscriptionHandlers.remove(destination);
    }

    /**
     * Waits for all outgoing messages to be sent, sends a DISCONNECT frame, waits
     * for the server to acknowledge the
     * DISCONNECT frame and finally closes the websocket connection.
     * If the outgoing messages are not sent within 10 seconds, they are skipped.
     */
    @Override
    public void close() {
        // hacky solution but works well enough
        for (int loopCount = 0; !messageQueue.isEmpty(); loopCount++) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                logger.warn("Thread sleep interrupted in loop", e);
                break;
            }
            if (loopCount > timeout) {
                logger.warn("Closing connection without sending lost messages (>10 seconds passed)");
                break;
            }
        }
        int disconnectReceipt = sendDisconnectFrame();
        receiptHandlers.put(disconnectReceipt, super::close);
    }

    /**
     * Called when the websocket connection is first established. The STOMP service
     * isn't ready yet.
     * If you need functionality as soon as the connection is usable, use
     * {@link #onConnected} instead.
     * Do not override unless you know what you are doing.
     */
    @Override
    public void onOpen(ServerHandshake handshake) {
        sendConnectFrame();
    }

    /**
     * Called when the STOMP connection is established. Can be overridden, as long
     * as `super.onConnected` is called.
     * You do not need to use this to send the first messages; they will be delayed
     * internally as needed.
     */
    public void onConnected() {
        connected = true;
        executeMessageQueue();
    }

    /**
     * Called for each frame received by the client.
     * Use this to handle other frames sent by the server which are not MESSAGE
     * frames sent to subscriptions.
     *
     * @param frame   the frame type
     * @param headers a key-value pair of frame headers
     * @param body    the frame body (can be an empty string, not null)
     */
    public abstract void onStompFrame(String frame, Map<String, String> headers, String body);

    /**
     * Called for each message received by the client. For STOMP messages, use
     * {@link #onStompFrame}.
     * Do not override this method unless you know what you are doing.
     *
     * @param message the raw message sent by the server
     */
    @Override
    public void onMessage(String message) {
        logger.trace("<<<{}", message);
        String[] parts = message.split("\n\n", 2);
        String[] headersArray = parts[0].split("\n");
        String frame = headersArray[0];
        Map<String, String> headers = new HashMap<>();
        for (int i = 1; i < headersArray.length; i++) {
            String[] header = headersArray[i].split(":");
            headers.put(header[0], header[1]);
        }
        // Strip trailing null byte
        String body = parts[1].replaceAll("\\x00$", "");
        switch (frame) {
            case "RECEIPT":
                String receivedReceiptString = headers.get("receipt-id");
                int receivedReceipt = Integer.parseInt(receivedReceiptString);
                if (!receiptHandlers.containsKey(receivedReceipt)) {
                    logger.warn("Received receipt {} but there was no handler", receivedReceiptString);
                    break;
                }
                receiptHandlers.get(receivedReceipt).run();
                break;
            case "MESSAGE":
                String destination = headers.get("destination");
                SubscriptionHandler handler = subscriptionHandlers.get(destination);
                if (handler == null) {
                    logger.error("Subscription handler for {} is null: {}", destination, subscriptionHandlers);
                    break;
                }
                handler.accept(headers, body);
                break;
            case "CONNECTED":
                onConnected();
                break;
            default:
                break;
        }
        onStompFrame(frame, headers, body);
    }
}
