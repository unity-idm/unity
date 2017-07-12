/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.utils.json;

import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.exceptions.EngineException;

/**
 * Map object to json ObjectNode using appropriate facility selected by type.
 * 
 * @author P.Piernik
 *
 */
@Component
public class JsonFormatter
{
	private JsonFormatterFacilitiesRegistry typesRegistry;

	public JsonFormatter(JsonFormatterFacilitiesRegistry typesRegistry)
	{
		super();
		this.typesRegistry = typesRegistry;
	}

	public ObjectNode toJson(String type, Object o) throws EngineException
	{
		if (typesRegistry.getAll().stream().map(f -> f.getName())
				.collect(Collectors.toList()).contains(type))
		{
			return typesRegistry.getByName(type).toJson(o);
		}

		return typesRegistry.getByName(DefaultJsonFormatterFacility.NAME).toJson(o);
	}

}
