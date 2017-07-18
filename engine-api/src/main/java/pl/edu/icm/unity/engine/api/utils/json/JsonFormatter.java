/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.utils.json;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.exceptions.EngineException;

/**
 * Maps objects to json
 * @author P.Piernik
 *
 */
@Component
public class JsonFormatter
{
	private JsonFormatterFacilitiesRegistry jsonFormatterRegistry;
	
	@Autowired
	public JsonFormatter(JsonFormatterFacilitiesRegistry jsonFormatterRegistry)
	{
		this.jsonFormatterRegistry = jsonFormatterRegistry;
	}
	
	public ObjectNode toJson(Token t) throws EngineException
	{
		ObjectNode node = Constants.MAPPER.valueToTree(t);
		((ObjectNode) node).set("contents",
				Constants.MAPPER.valueToTree(jsonFormatterRegistry.getFormatter(t.getType()).toJson(t.getContents())));
		return node;
	}

}
