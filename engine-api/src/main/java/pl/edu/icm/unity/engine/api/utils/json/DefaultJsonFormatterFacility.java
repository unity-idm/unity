/**
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.utils.json;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;

/**
 * Default formatter. 
 * @author P.Piernik
 *
 */
@Component
public class DefaultJsonFormatterFacility implements JsonFormatterFacility
{
	public static final String NAME = "DefaultJsonFormatter";

	@Override
	public String getDescription()
	{
		return "Default formatter";
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public ObjectNode toJson(Object o)
	{
		return Constants.MAPPER.valueToTree(o);
	}

}
