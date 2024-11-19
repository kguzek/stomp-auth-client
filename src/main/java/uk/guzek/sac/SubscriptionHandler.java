package uk.guzek.sac;

import java.util.Map;
import java.util.function.BiConsumer;

/** Function which takes the message headers and body and returns void */
public interface SubscriptionHandler extends BiConsumer<Map<String, String>, String> {}
