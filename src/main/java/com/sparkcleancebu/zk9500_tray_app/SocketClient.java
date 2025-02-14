package com.sparkcleancebu.zk9500_tray_app;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
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
@SuppressWarnings("exports")
public class SocketClient {
	private Session session;
	private WebSocketContainer container;

	private final URI uri;
	private final String channel;
	private final Map<String, Consumer<JSONObject>> events = new HashMap<>();
	private boolean disconnected;

	public SocketClient(URI uri, String channel) throws Exception {
		this.uri = uri;
		this.channel = channel;
		this.container = ContainerProvider.getWebSocketContainer();
		this.disconnected = false;

		container.connectToServer(this, uri);
		subscribe();
	}

	@OnOpen
	public void onOpen(Session session) {
		this.session = session;

		// initialize pre-defined events
		// connected successfully
		this.listen("pusher:connection_established", data -> {
			System.out.println("Connected to websocket server");
			System.out.println("Socket ID: " + data.getJSONObject("data").get("socket_id"));
		});

		// subscribed to a channel
		this.listen("pusher_internal:subscription_succeeded", data -> {
			String channelName = data.getString("channel");

			if (channel == channelName) {
				System.out.println("Successfully subscribed to the channel");
			}
		});

		// server ping
		this.listen("pusher:ping", data -> {
			try {
				send("pusher:pong", null);
				System.out.println("Sent Pong Response!");
			} catch (Exception e) {
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
				if (events.containsKey(eventName)) {
					events.get(eventName).accept(json);
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

	public String getChannel() {
		return channel;
	}

	private void subscribe() throws Exception {
		if (this.session != null && this.session.isOpen()) {
			send("pusher:subscribe", null);
		} else {
			throw new Exception("WebSocket session is not open. Unable to subscribe.");
		}
	}

	public void unsubscribe() throws Exception {
		if (this.session != null && this.session.isOpen()) {
			send("pusher:unsubscribe", null);
			System.out.println("Unsubscribed.");
		} else {
			throw new Exception("WebSocket session is not open. Unnable to unsubscribe.");
		}
	}

	public void listen(String eventName, Consumer<JSONObject> callback) {
		events.put(eventName, callback);
	}

	private void send(String event, JSONObject data) throws Exception {
		JSONObject content = new JSONObject();
		content.put("event", event);

		if (data == null) {
			data = new JSONObject();
		}

		if (channel != null && !channel.trim().isEmpty()) {
			data.put("channel", channel);
		}

		content.put("data", data);

		this.session.getBasicRemote().sendText(content.toString());
	}

	private void attemptReconnect() {
		if (disconnected) // manually disconnected
			return;

		try {
			System.out.println("Attempting to reconnect...");

			Thread.sleep(1000); // Wait

			container = ContainerProvider.getWebSocketContainer();
			container.connectToServer(this, this.uri);
			System.out.println("Reconnected successfully.");

			// Re-subscribe to channel
			subscribe();
		} catch (Exception e) {
			System.err.println("Reconnection failed: " + e.getMessage());
		}
	}

	public void disconnect() {
		if (session != null && session.isOpen()) {
			try {
				disconnected = true;
				session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Client disconnected"));
				System.out.println("WebSocket disconnected successfully.");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
