/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.vaadin.endpoint.common.api.services.authnlayout.configuration.elements;

import java.util.Properties;

public class PropertiesRepresentation
{
	public final String key;
	public final Properties propertiesValues;
	
	public PropertiesRepresentation(String key, Properties propertiesValues)
	{
		this.key = key;
		this.propertiesValues = propertiesValues;
	}
	
	public PropertiesRepresentation(String key)
	{
		this.key = key;
		this.propertiesValues = new Properties();
	}
	
	
}
