/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.utils;

import pl.edu.icm.unity.Constants;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Do-nothing {@link GenericObjectHandler}.
 * @author K. Benedyczak
 */
public class DefaultGenericObjectHandler implements GenericObjectHandler
{
	private String supportedType;
	private ObjectMapper jsonMapper = Constants.MAPPER;
	
	public DefaultGenericObjectHandler(String supportedType)
	{
		this.supportedType = supportedType;
	}

	@Override
	public String getSupportedType()
	{
		return supportedType;
	}

	@Override
	public byte[] updateBeforeImport(String type, String subType, String name, JsonNode contents) 
			throws JsonProcessingException
	{
		if (contents == null)
			return null;
		return jsonMapper.writeValueAsBytes(contents);
	}
}
