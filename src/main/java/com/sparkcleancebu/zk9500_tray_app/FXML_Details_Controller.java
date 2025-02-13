package com.sparkcleancebu.zk9500_tray_app;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class FXML_Details_Controller {
	@FXML
	private Label debugging;

	@FXML
	private Label name;

	@FXML
	private Label appId;

	@FXML
	private Label branch;

	@FXML
	private Label registeredAt;

	@FXML
	private Label host;

	@FXML
	private Label lastScan;

	@FXML
	private Label channelId;

	@FXML
	public void initialize() {
		debugging.setText(null);
		name.setText(null);
		appId.setText(null);
		branch.setText(null);
		registeredAt.setText(null);
		host.setText(null);
		lastScan.setText(null);
		channelId.setText(null);
	}

	public void display(Config config) throws Exception {
		name.setText(config.getName());
		appId.setText(config.getAppID());
		branch.setText(config.getBranch());
		registeredAt.setText(config.getVerifiedAt());
		host.setText(config.getHost());
	}

	public void debug(String content) {
		Platform.runLater(() -> {
			debugging.setText(content);
		});
	}

	public void scan(String content) {
		Platform.runLater(() -> {
			lastScan.setText(content);
		});
	}

	public void channel(String value) {
		Platform.runLater(() -> {
			channelId.setText(value);
		});
	}
}
