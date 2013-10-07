/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.generic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Base for implementations of {@link GenericEntityHandler}. 
 * @author K. Benedyczak
 */
public abstract class DefaultEntityHandler<T> implements GenericEntityHandler<T>
{
	protected ObjectMapper jsonMapper;
	protected String supportedType;
	protected Class<T> clazz;
	
	public DefaultEntityHandler(ObjectMapper jsonMapper, String supportedType, Class<T> clazz)
	{
		this.jsonMapper = jsonMapper;
		this.supportedType = supportedType;
		this.clazz = clazz;
	}

	@Override
	public String getType()
	{
		return supportedType;
	}

	@Override
	public Class<T> getModelClass()
	{
		return clazz;
	}

	@Override
	public byte[] updateBeforeImport(String name, JsonNode node) throws JsonProcessingException
	{
		if (node == null)
			return null;
		return jsonMapper.writeValueAsBytes(node);
	}
}
