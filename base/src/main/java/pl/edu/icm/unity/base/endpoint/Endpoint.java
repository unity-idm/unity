/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.endpoint;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.describedObject.NamedObject;

/**
 * Complete information allowing to create an endpoint
 * @author K. Benedyczak
 */
public class Endpoint implements NamedObject
{
	public static enum EndpointState { DEPLOYED, UNDEPLOYED};

	private String name; 
	private String typeId; 
	private String contextAddress;
	private EndpointConfiguration configuration;
	private long revision;
	private EndpointState state = EndpointState.DEPLOYED;

	
	public Endpoint(String name, String typeId, String contextAddress,
			EndpointConfiguration configuration, long revision)
	{
		this(name, typeId, contextAddress, configuration, revision, EndpointState.DEPLOYED);
	}
	
	public Endpoint(String name, String typeId, String contextAddress,
			EndpointConfiguration configuration, long revision, EndpointState state)
	{
		this.name = name;
		this.typeId = typeId;
		this.contextAddress = contextAddress;
		this.configuration = configuration;
		this.revision = revision;
		this.state = state;
	}

	@JsonCreator
	public Endpoint(ObjectNode root)
	{
		fromJson(root);
	}
	
	public EndpointConfiguration getConfiguration()
	{
		return configuration;
	}
	public String getTypeId()
	{
		return typeId;
	}
	@Override
	public String getName()
	{
		return name;
	}
	public String getContextAddress()
	{
		return contextAddress;
	}
	public long getRevision()
	{
		return revision;
	}
	public EndpointState getState()
	{
		return state;
	}

	@Override
	public String toString()
	{
		return "Endpoint [typeId=" + typeId + ", name="
				+ name + ", contextAddress=" + contextAddress + "revision=" + revision + "]";
	}
	
	@JsonValue
	public ObjectNode toJson()
	{
		ObjectNode root = Constants.MAPPER.createObjectNode();
		root.put("name", name);
		root.put("typeId", typeId);
		root.put("contextAddress", contextAddress);
		root.set("configuration", configuration.toJson());
		root.put("revision", revision);
		root.put("status", state.toString());
		return root;
	}

	private void fromJson(ObjectNode root)
	{
		name = root.get("name").asText();
		typeId = root.get("typeId").asText();
		contextAddress = root.get("contextAddress").asText();
		configuration = new EndpointConfiguration((ObjectNode) root.get("configuration"));
		if (root.has("revision"))
			revision = root.get("revision").asLong();
		if (root.has("status"))
		{
			state = EndpointState.valueOf(root.get("status").asText());
		}
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((configuration == null) ? 0 : configuration.hashCode());
		result = prime * result
				+ ((contextAddress == null) ? 0 : contextAddress.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + (int) (revision ^ (revision >>> 32));
		result = prime * result + ((typeId == null) ? 0 : typeId.hashCode());
		result = prime * result + ((state == null) ? 0 : state.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Endpoint other = (Endpoint) obj;
		if (configuration == null)
		{
			if (other.configuration != null)
				return false;
		} else if (!configuration.equals(other.configuration))
			return false;
		if (contextAddress == null)
		{
			if (other.contextAddress != null)
				return false;
		} else if (!contextAddress.equals(other.contextAddress))
			return false;
		if (name == null)
		{
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (revision != other.revision)
			return false;
		if (state != other.state)
			return false;
		if (typeId == null)
		{
			if (other.typeId != null)
				return false;
		} else if (!typeId.equals(other.typeId))
			return false;
		return true;
	}
}
