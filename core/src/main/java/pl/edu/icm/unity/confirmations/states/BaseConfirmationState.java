/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.confirmations.states;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.JsonSerializable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
/**
 * Contains common informations used during confirmation attribute and identity
 * @author P. Piernik
 *
 */
public class BaseConfirmationState implements JsonSerializable
{
	protected final ObjectMapper mapper = Constants.MAPPER;
	
	protected String facilityId;
	protected String owner;
	protected String type;
	protected String value;
	
	public String getFacilityId()
	{
		return facilityId;
	}
	public String getOwner()
	{
		return owner;
	}
	public String getType()
	{
		return type;
	}
	public String getValue()
	{
		return value;
	}
	private void setFacilityId(String facilityId)
	{
		this.facilityId = facilityId;
	}
	public void setOwner(String owner)
	{
		this.owner = owner;
	}
	public void setType(String type)
	{
		this.type = type;
	}
	public void setValue(String value)
	{
		this.value = value;
	}
	
	@Override
	public String getSerializedConfiguration() throws InternalException
	{
		try
		{
			return mapper.writeValueAsString(createState());
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't perform JSON serialization", e);
		}
	}
	
	protected ObjectNode createState()
	{
		ObjectNode state = mapper.createObjectNode();
		state.with("confirmationState");
		state.put("owner", getOwner());
		state.put("value", getValue());
		state.put("type", getType());
		state.put("facilityId", getFacilityId());
		return state;
	}
	
	@Override
	public void setSerializedConfiguration(String json) throws InternalException
	{
		try
		{
			ObjectNode main = mapper.readValue(json, ObjectNode.class);
			setOwner(main.get("owner").asText());
			setType(main.get("type").asText());
			setValue(main.get("value").asText());
			setFacilityId(main.get("facilityId").asText());
		} catch (Exception e)
		{
			throw new InternalException("Can't perform JSON deserialization", e);
		}

	}



}
