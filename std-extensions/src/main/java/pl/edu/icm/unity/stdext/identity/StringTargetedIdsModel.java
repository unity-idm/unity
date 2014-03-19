/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.identity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 * Stores simple strings as targeted values. Useful for {@link TargetedPersistentIdentity}.
 * @author K. Benedyczak
 */
public class StringTargetedIdsModel extends AbstractTargetedIdsDbModel<String>
{
	public StringTargetedIdsModel(ObjectMapper mapper, String value)
	{
		super(mapper, value);
	}

	@Override
	protected String deserializeValue(JsonNode node)
	{
		return node.asText();
	}

	@Override
	protected JsonNode serializeValue(String value)
	{
		return new TextNode(value);
	}
}
