/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.utils.json;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.utils.TypesRegistryBase;

/**
 * Maintains a simple registry of available {@link JsonFormatterFacility}ies.
 * 
 * @author P. Piernik
 */
@Component
public class JsonFormatterFacilitiesRegistry extends TypesRegistryBase<JsonFormatterFacility>
{
	@Autowired
	public JsonFormatterFacilitiesRegistry(List<JsonFormatterFacility> typeElements)
	{
		super(typeElements);
	}

	@Override
	protected String getId(JsonFormatterFacility from)
	{
		return from.getName();
	}
	
	public JsonFormatterFacility getFormatter(String type)
	{
		JsonFormatterFacility specificFormatter = getByNameOptional(type);
		return specificFormatter != null ? specificFormatter : 
			getByName(DefaultJsonFormatterFacility.NAME);
	}
}
