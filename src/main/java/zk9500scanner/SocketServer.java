package zk9500scanner;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONArray;
import org.json.JSONObject;

public class SocketServer extends WebSocketServer {
	private AtomicBoolean running = new AtomicBoolean(false);

	public Consumer<Integer> onRegistrationStart = null;
	public Consumer<JSONArray> onInitialize = null;

	public SocketServer(int port) throws UnknownHostException {
		super(new InetSocketAddress(port));
	}

	public SocketServer(InetSocketAddress address) {
		super(address);
	}

	public SocketServer(int port, Draft_6455 draft) {
		super(new InetSocketAddress(port), Collections.<Draft>singletonList(draft));
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		Main.log.info(conn.getRemoteSocketAddress().getAddress().getHostAddress() + " has entered the connection");
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		Main.log.info(conn + " has disconnected");
	}

	@Override
	public void onMessage(WebSocket conn, String message) {
		processMessage(new JSONObject(message));
	}

	@Override
	public void onMessage(WebSocket conn, ByteBuffer message) {
		processMessage(new JSONObject(message));
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {
		running.set(false);
		Main.log.severe("Websocket error: " + ex.getLocalizedMessage());
	}

	@Override
	public void onStart() {
		running.set(true);
		Main.log.info("WebSocket server started at port " + System.getProperty("wsport", "1234"));
		setConnectionLostTimeout(0);
		setConnectionLostTimeout(100);
	}

	public void send(JSONObject json) {
		String message = json.toString();
		broadcast(message);
	}

	public boolean isRunning() {
		return running.get();
	}

	private void processMessage(JSONObject data) {
		try {
			String eventName = data.getString("event");

			// event hooks
			if (eventName.equals("registration-start") && onRegistrationStart != null) {
				onRegistrationStart.accept(data.getInt("id"));
			} else if (eventName.equals("library-init") && onInitialize != null) {
				onInitialize.accept(data.getJSONArray("fingerprints"));
			}
		} catch (Exception e) {
			Main.log.severe(e.getLocalizedMessage());
		}
	}
}