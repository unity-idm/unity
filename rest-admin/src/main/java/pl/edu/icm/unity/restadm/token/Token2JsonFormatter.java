/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm.token;

import static pl.edu.icm.unity.Constants.MAPPER;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.imunity.rest.api.types.basic.RestToken;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.engine.api.utils.json.TokenContentsJsonSerializer;
import pl.edu.icm.unity.exceptions.EngineException;

/**
 * Maps {@link Token}s to json using respective token contents serialization. 
 * @author P.Piernik
 *
 */
@Component
public class Token2JsonFormatter
{
	private JsonFormatterFacilitiesRegistry jsonFormatterRegistry;
	
	@Autowired
	public Token2JsonFormatter(JsonFormatterFacilitiesRegistry jsonFormatterRegistry)
	{
		this.jsonFormatterRegistry = jsonFormatterRegistry;
	}
	
	public ObjectNode toJson(RestToken t) throws EngineException
	{
		TokenContentsJsonSerializer formatter = 
				jsonFormatterRegistry.getFormatter(t.type);
		ObjectNode node = MAPPER.valueToTree(t);
		node.set("contents", MAPPER.valueToTree(formatter.toJson(t.contents)));
		return node;
	}
}
