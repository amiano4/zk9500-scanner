package com.sparkcleancebu.zk9500_tray_app;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.json.JSONObject;

import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.CloseReason;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;

@ClientEndpoint
public class SocketClient {
	private Session session;
	private final Set<String> subscribedChannels = new HashSet<>();
	private final Map<String, Consumer<JSONObject>> eventListeners = new HashMap<>();
	private URI uri;

	public SocketClient(URI uri) {
		this.uri = uri;
	}

	@OnOpen
	public void onOpen(Session session) {
		this.session = session;
		System.out.println("Connecting to WebSocket server...");

		// initialize pre-defined events
		// connected successfully
		this.listen("pusher:connection_established", data -> {
			System.out.println("Connection established with socket ID: " + data.getJSONObject("data").get("socket_id"));
		});

		// subscribed to a channel
		this.listen("pusher_internal:subscription_succeeded", data -> {
			String channel = data.getString("channel");
			subscribedChannels.add(channel);
			System.out.println("Subscribing to channel: " + channel);
		});

		// server ping
		this.listen("pusher:ping", data -> {
			JSONObject pongMessage = new JSONObject();
			pongMessage.put("event", "pusher:pong");
			try {
				this.session.getBasicRemote().sendText(pongMessage.toString());
				System.out.println("Sent Pong Response!");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
	}

	@OnMessage
	public void onMessage(String message) {
		try {
			JSONObject json = new JSONObject(message);

			if (json.has("event")) {
				String eventName = json.getString("event");

				// Ensure "data" is always a JSONObject
				if (json.has("data")) {
					Object rawData = json.get("data");

					if (rawData instanceof String) {
						json.put("data", new JSONObject((String) rawData)); // Replace string with parsed JSON
					}
				}

				// Pass the full JSON message (with corrected data) to the event listener
				if (eventListeners.containsKey(eventName)) {
					eventListeners.get(eventName).accept(json);
				}
			}
		} catch (Exception e) {
			System.err.println("Error parsing message: " + e.getMessage());
		}
	}

	@OnClose
	public void onClose(Session session, CloseReason reason) {
		System.out.println("Connection closed: " + reason);
		attemptReconnect();
	}

	@OnError
	public void onError(Session session, Throwable throwable) {
		System.out.println("Error: " + throwable.getMessage());
		attemptReconnect();
	}

	public void subscribe(String channelName) throws Exception {
		if (!subscribedChannels.contains(channelName)) {
			if (this.session != null && this.session.isOpen()) {
				JSONObject newSubscription = new JSONObject();
				newSubscription.put("event", "pusher:subscribe");

				JSONObject data = new JSONObject();
				data.put("channel", channelName);

				newSubscription.put("data", data);

				this.session.getBasicRemote().sendText(newSubscription.toString());
			} else {
				throw new Exception("WebSocket session is not open. Cannot subscribe to channel: " + channelName);
			}
		} else {
			System.out.println("Already subscribed to channel: " + channelName);
		}
	}

	public void unsubscribe(String channelName) throws Exception {
		if (subscribedChannels.contains(channelName)) {
			if (this.session != null && this.session.isOpen()) {
				JSONObject unsubscribeMessage = new JSONObject();
				unsubscribeMessage.put("event", "pusher:unsubscribe");

				JSONObject data = new JSONObject();
				data.put("channel", channelName);

				unsubscribeMessage.put("data", data);

				this.session.getBasicRemote().sendText(unsubscribeMessage.toString());
				subscribedChannels.remove(channelName);
				System.out.println("Unsubscribed from channel: " + channelName);
			} else {
				throw new Exception("WebSocket session is not open. Cannot unsubscribe from channel: " + channelName);
			}
		} else {
			System.out.println("Not subscribed to channel: " + channelName);
		}
	}

	@SuppressWarnings("exports")
	public void listen(String eventName, Consumer<JSONObject> callback) {
		eventListeners.put(eventName, callback);
	}

	public Set<String> getSubscriptions() {
		return new HashSet<>(subscribedChannels);
	}

	private void attemptReconnect() {
		try {
			System.out.println("Attempting to reconnect...");

			Thread.sleep(1000); // Wait 5 seconds before reconnecting

			WebSocketContainer container = ContainerProvider.getWebSocketContainer();
			container.connectToServer(this, this.uri);
			System.out.println("Reconnected successfully.");

			// Re-subscribe to channels
			for (String channel : subscribedChannels) {
				subscribe(channel);
			}
		} catch (Exception e) {
			System.err.println("Reconnection failed: " + e.getMessage());
		}
	}
}
