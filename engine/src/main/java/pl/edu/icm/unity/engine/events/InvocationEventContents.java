/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.events;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.InternalException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Describes method invocation. Provides to/from JSON serialization.
 * @author K. Benedyczak
 */
public class InvocationEventContents
{
	private String method;
	private String interfaceName;
	private String exception;
	private static ObjectMapper mapper = Constants.MAPPER;
	
	public InvocationEventContents(String method, String interfaceName)
	{
		this.method = method;
		this.interfaceName = interfaceName;
	}
	
	public InvocationEventContents(String method, String interfaceName, String exception)
	{
		this.method = method;
		this.interfaceName = interfaceName;
		this.exception = exception;
	}

	public InvocationEventContents()
	{
	}

	public String toJson()
	{
		ObjectNode root = mapper.createObjectNode();
		root.put("method", method);
		root.put("interfaceName", interfaceName);
		root.put("exception", exception);
		try
		{
			return mapper.writeValueAsString(root);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize InvocationEventContents to Json", e);
		}
	}
	
	public void fromJson(String json)
	{
		JsonNode root;
		try
		{
			root = mapper.readTree(json);
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize InvocationEventContents from Json", e);
		}
		method = root.get("method").asText();
		interfaceName = root.get("interfaceName").asText();
		JsonNode jn = root.get("exception");
		exception = jn == null ? null : jn.asText();
	}

	public String getMethod()
	{
		return method;
	}

	public String getInterfaceName()
	{
		return interfaceName;
	}

	public String getException()
	{
		return exception;
	}
}
