/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.persistence.sitewhere.internal;

import java.io.File;

import org.openhab.config.core.ConfigDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Manages SiteWhere configuration options.
 * 
 * @author Derek
 */
public class SiteWhereConfigurationManager {

	/** Static logger instance */
	private static final Logger LOGGER = LoggerFactory
			.getLogger(SiteWhereConfigurationManager.class);

	/** SiteWhere folder name */
	private static final String SITEWHERE_FOLDER = "sitewhere";

	/** SiteWhere config file name */
	private static final String SITEWHERE_CONFIGURATION = "sitewhere.json";

	/** Singleton instance */
	private static SiteWhereConfigurationManager INSTANCE;

	/** Singleton object mapper for JSON marshaling */
	private static ObjectMapper MAPPER = new ObjectMapper();

	/** SiteWhere configuration settings */
	private SiteWhereConfiguration configuration;

	/** Field mapper */
	private FieldMapper fieldMapper;

	private SiteWhereConfigurationManager() {
		File config = resolveConfigurationFile();
		try {
			this.configuration = MAPPER.readValue(config,
					SiteWhereConfiguration.class);
			this.fieldMapper = new FieldMapper(configuration);
			LOGGER.info("Loaded SiteWhere configuration file from: "
					+ config.getAbsolutePath());
		} catch (Throwable e) {
			throw new RuntimeException(
					"Unable to parse JSON from SiteWhere openHAB configuration file.",
					e);
		}
	}

	/**
	 * Resolve the XML configuration file for configuring SiteWhere
	 * connectivity.
	 * 
	 * @return
	 */
	protected File resolveConfigurationFile() {
		String root = ConfigDispatcher.getConfigFolder();
		if (root == null) {
			throw new RuntimeException("Unable to locate configuration folder.");
		}
		File sitewhere = new File(root, SITEWHERE_FOLDER);
		if (!sitewhere.exists()) {
			throw new RuntimeException(
					"Unable to locate SiteWhere configuration folder. Location: "
							+ sitewhere.getAbsolutePath());
		}
		File config = new File(sitewhere, SITEWHERE_CONFIGURATION);
		if (!config.exists()) {
			throw new RuntimeException(
					"Unable to locate SiteWhere configuration file. Location: "
							+ sitewhere.getAbsolutePath());
		}
		return config;
	}

	public static SiteWhereConfigurationManager getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new SiteWhereConfigurationManager();
		}
		return INSTANCE;
	}

	public SiteWhereConfiguration getConfiguration() {
		return configuration;
	}

	public void setConfiguration(SiteWhereConfiguration configuration) {
		this.configuration = configuration;
	}

	public FieldMapper getFieldMapper() {
		return fieldMapper;
	}

	public void setFieldMapper(FieldMapper fieldMapper) {
		this.fieldMapper = fieldMapper;
	}
}