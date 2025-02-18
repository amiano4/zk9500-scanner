package com.sparkcleancebu.zk9500_tray_app;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class FXML_Details_Controller {
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
	public void initialize() {
		name.setText(null);
		appId.setText(null);
		branch.setText(null);
		registeredAt.setText(null);
		host.setText(null);
	}

	public void display(Config config) throws Exception {
		name.setText(config.getName());
		appId.setText(config.getAppID());
		branch.setText(config.getBranch());
		registeredAt.setText(config.getVerifiedAt());
		host.setText(config.getHost());
	}
}
