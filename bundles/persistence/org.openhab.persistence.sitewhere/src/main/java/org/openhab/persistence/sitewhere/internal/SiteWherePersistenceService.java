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
import java.util.Dictionary;
import java.util.Properties;

import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.persistence.FilterCriteria;
import org.openhab.core.persistence.HistoricItem;
import org.openhab.core.persistence.PersistenceService;
import org.openhab.core.persistence.QueryablePersistenceService;
import org.openhab.core.types.State;
import org.openhab.persistence.sitewhere.internal.SiteWhereConfiguration.Connection;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sitewhere.agent.Agent;
import com.sitewhere.agent.IAgentConfiguration;
import com.sitewhere.agent.SiteWhereAgentException;
import com.sitewhere.rest.client.SiteWhereClient;
import com.sitewhere.rest.model.system.Version;
import com.sitewhere.spi.ISiteWhereClient;
import com.sitewhere.spi.SiteWhereException;

/**
 * Implementation of {@link PersistenceService} for interacting with SiteWhere.
 * 
 * @author Derek Adams
 */
public class SiteWherePersistenceService implements
		QueryablePersistenceService, ManagedService {

	/** Static logger instance */
	private static final Logger LOGGER = LoggerFactory
			.getLogger(SiteWherePersistenceService.class);

	/** Alert type indicator for a DateTimeType */
	public static final String TYPE_OPENHAB_DATETIME = "openhab.datetime";

	/** Alert type indicator for a OnOffType */
	public static final String TYPE_OPENHAB_ONOFF = "openhab.onoff";

	/** Alert type indicator for a OpenClosedType */
	public static final String TYPE_OPENHAB_OPENCLOSED = "openhab.openclosed";

	/** Alert type indicator for a StringType */
	public static final String TYPE_OPENHAB_STRING = "openhab.string";

	/** Service name */
	private static final String SERVICE_NAME = "sitewhere";

	/** REST client used to interact with SiteWhere */
	private ISiteWhereClient client;

	/** SiteWhere MQTT agent */
	private Agent agent;

	/** Command processor that interacts with SiteWhere */
	private OpenHabCommandProcessor sitewhere;

	/** Reference to openHAB item registry */
	protected ItemRegistry itemRegistry;

	/** Event publisher for openHAB */
	private EventPublisher eventPublisher;

	/** Indicates if the service has been configured */
	protected boolean configured = false;

	/** Indicates if REST connectivity has been verified */
	protected boolean restVerified = false;

	/** Indicates if SiteWhere agent is connected */
	protected boolean agentConnected = false;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openhab.core.persistence.PersistenceService#getName()
	 */
	public String getName() {
		return SERVICE_NAME;
	}

	/**
	 * OSGI setter for {@link EventPublisher}.
	 * 
	 * @param publisher
	 */
	public void setEventPublisher(EventPublisher publisher) {
		this.eventPublisher = publisher;
	}

	/**
	 * OSGI unsetter for {@link EventPublisher}.
	 * 
	 * @param publisher
	 */
	public void unsetEventPublisher(EventPublisher publisher) {
		this.eventPublisher = null;
	}

	/**
	 * Method called when bundle is activated.
	 */
	public void activate() {
		LOGGER.debug("Activate method called on persistence service.");
		try {
			SiteWhereConfigurationManager.getInstance();
			setConfigured(true);
			validateSiteWhereClient();
			validateSiteWhereAgent();
		} catch (Throwable t) {
			LOGGER.error("SiteWhere activation failed.", t);
			setConfigured(false);
		}
	}

	/**
	 * Method called when bundle is deactivated.
	 */
	public void deactivate() {
		LOGGER.debug("Deactivate method called on persistence service.");
		setConfigured(false);
	}

	/**
	 * Set reference to item registry.
	 * 
	 * @param itemRegistry
	 */
	public void setItemRegistry(ItemRegistry itemRegistry) {
		this.itemRegistry = itemRegistry;
		LOGGER.debug("Item registry set for SiteWhere persistence.");
	}

	/**
	 * Unset reference to item registry.
	 * 
	 * @param itemRegistry
	 */
	public void unsetItemRegistry(ItemRegistry itemRegistry) {
		this.itemRegistry = null;
	}

	/**
	 * Validate that client is able to interact with SiteWhere server.
	 * 
	 * @return
	 */
	protected boolean validateSiteWhereClient() {
		try {
			SiteWhereConfiguration config = SiteWhereConfigurationManager
					.getInstance().getConfiguration();
			this.client = new SiteWhereClient(config.getConnection()
					.getRestApiUrl(), config.getConnection()
					.getRestApiUsername(), config.getConnection()
					.getRestApiPassword(), 2000);

			Version version = client.getSiteWhereVersion();
			LOGGER.info("Connected to SiteWhere "
					+ version.getVersionIdentifier() + " "
					+ version.getEditionIdentifier());
			setRestVerified(true);
			return true;
		} catch (SiteWhereException e) {
			this.client = null;
			setRestVerified(false);
			LOGGER.error("Unable to connect with SiteWhere server. Verify configuration.");
			return false;
		}
	}

	/**
	 * Validate that the SiteWhere MQTT agent can connect.
	 * 
	 * @return
	 */
	protected boolean validateSiteWhereAgent() {
		Connection connection = SiteWhereConfigurationManager.getInstance()
				.getConfiguration().getConnection();

		Properties props = new Properties();
		props.setProperty(IAgentConfiguration.COMMAND_PROCESSOR_CLASSNAME, "");
		props.setProperty(IAgentConfiguration.DEVICE_SPECIFICATION_TOKEN,
				"5a95f3f2-96f0-47f9-b98d-f5c081d01948");
		props.setProperty(IAgentConfiguration.DEVICE_HARDWARE_ID,
				SiteWhereConfigurationManager.getInstance().getConfiguration()
						.getDefaultHardwareId());
		props.setProperty(IAgentConfiguration.MQTT_HOSTNAME,
				connection.getMqttHost());
		props.setProperty(IAgentConfiguration.MQTT_PORT,
				String.valueOf(connection.getMqttPort()));

		this.agent = new Agent();
		agent.load(props);

		this.sitewhere = new OpenHabCommandProcessor(eventPublisher);

		try {
			agent.start(sitewhere);
			setAgentConnected(true);
			return true;
		} catch (SiteWhereAgentException e) {
			setAgentConnected(false);
			LOGGER.error("Unable to start SiteWhere MQTT agent.", e);
			return false;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
	 */
	public void updated(Dictionary<String, ?> config)
			throws ConfigurationException {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.openhab.core.persistence.PersistenceService#store(org.openhab.core
	 * .items.Item)
	 */
	public void store(Item item) {
		store(item, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.openhab.core.persistence.PersistenceService#store(org.openhab.core
	 * .items.Item, java.lang.String)
	 */
	public void store(Item item, String alias) {
		if (isAgentConnected()) {
			String hwid = SiteWhereConfigurationManager.getInstance()
					.getFieldMapper().getHardwareIdForItem(item.getName());
			if (hwid == null) {
				LOGGER.warn("No mapping found for field: " + item.getName());
				return;
			}

			try {
				State state = item.getState();
				if (state instanceof PercentType) {
					addDeviceMeasurement(hwid, item, ((PercentType) state)
							.toBigDecimal().doubleValue());
				} else if (state instanceof DecimalType) {
					addDeviceMeasurement(hwid, item, ((DecimalType) state)
							.toBigDecimal().doubleValue());
				} else if (state instanceof DateTimeType) {
					addDeviceAlert(hwid, item, (DateTimeType) state);
				} else if (state instanceof OnOffType) {
					addDeviceAlert(hwid, item, (OnOffType) state);
				} else if (state instanceof OpenClosedType) {
					addDeviceAlert(hwid, item, (OpenClosedType) state);
				} else if (state instanceof StringType) {
					addDeviceAlert(hwid, item, (StringType) state);
				}

				/*
				 * else if (state instanceof PointType) {
				 * addDeviceLocation(batch, info, (PointType) state); }
				 */

				else {
					LOGGER.warn("Unable to store item of type: "
							+ item.getState().getClass().getSimpleName());
					return;
				}
			} catch (SiteWhereAgentException e) {
				LOGGER.warn("Unable to store item: " + item.getName(), e);
			}
		}
	}

	/**
	 * Send a device measurement to SiteWhere.
	 * 
	 * @param hardwareId
	 * @param item
	 * @param value
	 * @throws SiteWhereAgentException
	 */
	protected void addDeviceMeasurement(String hardwareId, Item item,
			double value) throws SiteWhereAgentException {
		sitewhere.sendMeasurement(hardwareId, item.getName(), value, null);
	}

	/**
	 * Send an alert for a {@link DateTimeType}.
	 * 
	 * @param hardwareId
	 * @param item
	 * @param value
	 * @throws SiteWhereAgentException
	 */
	protected void addDeviceAlert(String hardwareId, Item item,
			DateTimeType value) throws SiteWhereAgentException {
		addDeviceAlert(hardwareId, item, TYPE_OPENHAB_DATETIME,
				value.toString());
	}

	/**
	 * Send an alert for an {@link OnOffType}.
	 * 
	 * @param hardwareId
	 * @param item
	 * @param value
	 * @throws SiteWhereAgentException
	 */
	protected void addDeviceAlert(String hardwareId, Item item, OnOffType value)
			throws SiteWhereAgentException {
		addDeviceAlert(hardwareId, item, TYPE_OPENHAB_ONOFF, value.toString());
	}

	/**
	 * Send an alert for an {@link OpenClosedType}.
	 * 
	 * @param hardwareId
	 * @param item
	 * @param value
	 * @throws SiteWhereAgentException
	 */
	protected void addDeviceAlert(String hardwareId, Item item,
			OpenClosedType value) throws SiteWhereAgentException {
		addDeviceAlert(hardwareId, item, TYPE_OPENHAB_OPENCLOSED,
				value.toString());
	}

	/**
	 * Send an alert for a {@link StringType}.
	 * 
	 * @param hardwareId
	 * @param item
	 * @param value
	 * @throws SiteWhereAgentException
	 */
	protected void addDeviceAlert(String hardwareId, Item item, StringType value)
			throws SiteWhereAgentException {
		addDeviceAlert(hardwareId, item, TYPE_OPENHAB_STRING, value.toString());
	}

	/**
	 * Send a device alert to SiteWhere.
	 * 
	 * @param hardwareId
	 * @param item
	 * @param type
	 * @param value
	 * @throws SiteWhereAgentException
	 */
	protected void addDeviceAlert(String hardwareId, Item item, String type,
			String value) throws SiteWhereAgentException {
		String habType = type + ":" + item.getName();
		sitewhere.sendAlert(hardwareId, habType, value, null);
	}

	/**
	 * Add a device location to the given batch.
	 * 
	 * @param batch
	 * @param info
	 * @param point
	 */
	/*
	 * protected void addDeviceLocation(DeviceEventBatch batch,
	 * SiteWhereEventInfo info, PointType point) { DeviceLocationCreateRequest
	 * loc = new DeviceLocationCreateRequest();
	 * loc.setLatitude(point.getLatitude().doubleValue());
	 * loc.setLongitude(point.getLongitude().doubleValue());
	 * loc.setElevation(point.getAltitude().doubleValue());
	 * loc.getMetadata().put("gravity", point.getGravity().toString());
	 * loc.setEventDate(new Date()); loc.setUpdateState(true);
	 * batch.getLocations().add(loc); }
	 */

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.openhab.core.persistence.QueryablePersistenceService#query(org.openhab
	 * .core.persistence.FilterCriteria)
	 */
	public Iterable<HistoricItem> query(FilterCriteria filter) {
		if (client != null) {
			LOGGER.debug("Query method called on persistence service. Item:"
					+ filter.getItemName() + " Page:" + filter.getPageNumber()
					+ " PageSize" + filter.getPageSize() + " Start:"
					+ filter.getBeginDate() + " End" + filter.getEndDate()
					+ " State:" + filter.getState() + " Order:"
					+ filter.getOrdering() + " Operator:"
					+ filter.getOperator());
		}
		return new ArrayList<HistoricItem>();
	}

	public boolean isConfigured() {
		return configured;
	}

	public void setConfigured(boolean configured) {
		this.configured = configured;
	}

	public boolean isRestVerified() {
		return restVerified;
	}

	public void setRestVerified(boolean restVerified) {
		this.restVerified = restVerified;
	}

	public boolean isAgentConnected() {
		return agentConnected;
	}

	public void setAgentConnected(boolean agentConnected) {
		this.agentConnected = agentConnected;
	}
}