/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.vaadin.auth.services.layout.configuration.elements;

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
