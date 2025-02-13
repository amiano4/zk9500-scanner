package com.sparkcleancebu.zk9500_tray_app;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
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
@SuppressWarnings("exports")
public class SocketClient {
	private Session session;
	private URI uri;
	private WebSocketContainer container;

	private final Set<Channel> channels = new HashSet<>();
	private final Map<String, Consumer<JSONObject>> events = new HashMap<>();

	public SocketClient(URI uri) throws Exception {
		this.uri = uri;
		this.container = ContainerProvider.getWebSocketContainer();
		connect();
	}

	private void connect() throws Exception {
		System.out.println("Connecting to WebSocket server...");
		container.connectToServer(this, uri);
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
			System.out.println("Subscribing to channel: " + channel);
		});

		// server ping
		this.listen("pusher:ping", data -> {
			try {
				send("pusher:pong");
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

	public Channel subscribe(String channelName) throws Exception {
		return subscribe(new Channel(this, channelName));
	}

	public Channel subscribe(Channel channel) throws Exception {
		String channelName = channel.getName();

		if (!channels.contains(channel)) {
			if (this.session != null && this.session.isOpen()) {
				send("pusher:subscribe", new JSONObject(), channelName);
				channels.add(channel);
			} else {
				throw new Exception("WebSocket session is not open. Cannot subscribe to channel: " + channelName);
			}
		} else {
			System.out.println("Already subscribed to channel: " + channelName);
		}

		return channel;
	}

	public void unsubscribe(String channelName) throws Exception {
		unsubscribe(new Channel(this, channelName));
	}

	public void unsubscribe(Channel channel) throws Exception {
		String channelName = channel.getName();

		if (channels.contains(channel)) {
			if (this.session != null && this.session.isOpen()) {
				send("pusher:unsubscribe", null, channelName);
				channels.remove(channel);
				System.out.println("Unsubscribed from channel: " + channelName);
			} else {
				throw new Exception("WebSocket session is not open. Cannot unsubscribe from channel: " + channelName);
			}
		} else {
			System.out.println("Not subscribed to channel: " + channelName);
		}
	}

	public void listen(String eventName, Consumer<JSONObject> callback) {
		events.put(eventName, callback);
	}

	public Set<Channel> getSubscriptions() {
		return new HashSet<>(channels);
	}

	public void send(String event) throws Exception {
		send(event, null, null);
	}

	public void send(String event, JSONObject data) throws Exception {
		send(event, data, null);
	}

	public void send(String event, JSONObject data, String channel) throws Exception {
		JSONObject content = new JSONObject();
		content.put("event", event);

		if (data == null) {
			data = new JSONObject();
		}

		if (channel != null && !channel.trim().isEmpty()) {
			data.put("channel", channel);
			content.put("data", data);
		}

		this.session.getBasicRemote().sendText(content.toString());
	}

	private void attemptReconnect() {
		try {
			System.out.println("Attempting to reconnect...");

			Thread.sleep(1000); // Wait

			WebSocketContainer container = ContainerProvider.getWebSocketContainer();
			container.connectToServer(this, this.uri);
			System.out.println("Reconnected successfully.");

			// Re-subscribe to channels
			for (Channel channel : channels) {
				subscribe(channel);
			}
		} catch (Exception e) {
			System.err.println("Reconnection failed: " + e.getMessage());
		}
	}

	public static class Channel {
		private final SocketClient client;
		private final String name;

		public Channel(SocketClient client, String name) {
			this.client = client;
			this.name = name;
		}

		public SocketClient getClient() {
			return this.client;
		}

		public String getName() {
			return this.name;
		}

		public void listen(String eventName, Consumer<JSONObject> callback) {
			client.listen(eventName, callback);
		}

		public void send(String event) throws Exception {
			client.send(event, null, name);
		}

		public void send(String event, JSONObject data) throws Exception {
			client.send(event, data, name);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null || getClass() != obj.getClass())
				return false;
			Channel that = (Channel) obj;
			return Objects.equals(name, that.name) && Objects.equals(client, that.client);
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, client);
		}
	}
}
