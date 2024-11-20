/**
 * STOMP Auth Client - A simple STOMP client for Java with authentication support
 * Copyright © 2024 by Konrad Guzek <konrad@guzek.uk>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package uk.guzek.sac;

import java.util.Map;
import java.util.function.BiConsumer;

/** Function which takes the message headers and body and returns void */
public interface SubscriptionHandler extends BiConsumer<Map<String, String>, String> {
}
