/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.persistence.sitewhere.internal;

import java.util.ArrayList;
import java.util.List;

/**
 * Configures how openHAB items are mapped to SiteWhere devices.
 * 
 * @author Derek
 */
public class SiteWhereConfiguration {

	/** Default hardware id for unmapped items */
	private String defaultHardwareId;

	/** Connection Information */
	private Connection connection;

	/** List of item/device mappings */
	private List<Mapping> mappings = new ArrayList<Mapping>();

	public String getDefaultHardwareId() {
		return defaultHardwareId;
	}

	public void setDefaultHardwareId(String defaultHardwareId) {
		this.defaultHardwareId = defaultHardwareId;
	}

	public Connection getConnection() {
		return connection;
	}

	public void setConnection(Connection connection) {
		this.connection = connection;
	}

	public List<Mapping> getMappings() {
		return mappings;
	}

	public void setMappings(List<Mapping> mappings) {
		this.mappings = mappings;
	}

	/**
	 * Connection parameters for MQTT connectivity.
	 * 
	 * @author Derek
	 */
	public static class Connection {

		/** REST API URL */
		private String restApiUrl = "http://localhost:9090/sitewhere/api/";

		/** REST API username */
		private String restApiUsername = "admin";

		/** REST API password */
		private String restApiPassword = "password";

		/** Hostname for MQTT server */
		private String mqttHost = "localhost";

		/** Port for MQTT server */
		private int mqttPort = 1883;

		public String getRestApiUrl() {
			return restApiUrl;
		}

		public void setRestApiUrl(String restApiUrl) {
			this.restApiUrl = restApiUrl;
		}

		public String getRestApiUsername() {
			return restApiUsername;
		}

		public void setRestApiUsername(String restApiUsername) {
			this.restApiUsername = restApiUsername;
		}

		public String getRestApiPassword() {
			return restApiPassword;
		}

		public void setRestApiPassword(String restApiPassword) {
			this.restApiPassword = restApiPassword;
		}

		public String getMqttHost() {
			return mqttHost;
		}

		public void setMqttHost(String mqttHost) {
			this.mqttHost = mqttHost;
		}

		public int getMqttPort() {
			return mqttPort;
		}

		public void setMqttPort(int mqttPort) {
			this.mqttPort = mqttPort;
		}
	}

	/**
	 * Maps an openHAB item to a SiteWhere device.
	 * 
	 * @author Derek
	 */
	public static class Mapping {

		/** OpenHAB item name */
		private String itemName;

		/** Hardware id of SiteWhere device */
		private String hardwareId;

		public String getItemName() {
			return itemName;
		}

		public void setItemName(String itemName) {
			this.itemName = itemName;
		}

		public String getHardwareId() {
			return hardwareId;
		}

		public void setHardwareId(String hardwareId) {
			this.hardwareId = hardwareId;
		}
	}
}