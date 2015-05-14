/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.persistence.sitewhere.internal;

import java.util.HashMap;
import java.util.Map;

import org.openhab.persistence.sitewhere.internal.SiteWhereConfiguration.Mapping;

/**
 * Helper class for mapping item names to hardware ids.
 * 
 * @author Derek
 */
public class FieldMapper {

	/** Wrapped configuration */
	private SiteWhereConfiguration configuration;

	/** Map of item names to hardware ids */
	private Map<String, String> itemToHardwareId = new HashMap<String, String>();

	public FieldMapper(SiteWhereConfiguration configuration) {
		this.configuration = configuration;
		initialize();
	}

	/**
	 * Perform initialization.
	 */
	protected void initialize() {
		itemToHardwareId.clear();
		for (Mapping mapping : configuration.getMappings()) {
			itemToHardwareId
					.put(mapping.getItemName(), mapping.getHardwareId());
		}
	}

	/**
	 * Get hardware id for a given item name.
	 * 
	 * @param itemName
	 * @return
	 */
	public String getHardwareIdForItem(String itemName) {
		String hwid = itemToHardwareId.get(itemName);
		if (hwid == null) {
			return configuration.getDefaultHardwareId();
		}
		return hwid;
	}

	protected SiteWhereConfiguration getConfiguration() {
		return configuration;
	}

	protected void setConfiguration(SiteWhereConfiguration configuration) {
		this.configuration = configuration;
	}
}