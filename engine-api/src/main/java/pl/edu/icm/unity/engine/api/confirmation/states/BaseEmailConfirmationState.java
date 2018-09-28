/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.confirmation.states;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;

/**
 * Contains common informations used during confirmation
 * @author P. Piernik
 *
 */
public class BaseEmailConfirmationState
{
	protected final ObjectMapper mapper = Constants.MAPPER;
	
	protected String facilityId;
	protected String type;
	protected String value;
	protected String locale;
	
	public BaseEmailConfirmationState(String facilityId, String type, String value, String locale)
	{
		this.type = type;
		this.value = value;
		this.locale = locale;
		this.facilityId = facilityId;
	}
	
	public BaseEmailConfirmationState(String serializedState) throws WrongArgumentException
	{
		setSerializedConfiguration(serializedState);
	}

	protected BaseEmailConfirmationState()
	{
	}

	public String getFacilityId()
	{
		return facilityId;
	}
	public String getType()
	{
		return type;
	}
	public String getValue()
	{
		return value;
	}
	public String getLocale()
	{
		return locale;
	}
	
	public String getSerializedConfiguration() throws InternalException
	{
		return JsonUtil.serialize(createState());
	}
	
	protected ObjectNode createState()
	{
		ObjectNode state = mapper.createObjectNode();
		state.with("confirmationState");
		state.put("value", getValue());
		state.put("type", getType());
		state.put("facilityId", getFacilityId());
		state.put("locale", getLocale());
		return state;
	}
	
	protected void setSerializedConfiguration(String json)
	{
		setSerializedConfiguration(JsonUtil.parse(json));
	}
	
	protected void setSerializedConfiguration(ObjectNode main)
	{
		try
		{
			type = main.get("type").asText();
			value = main.get("value").asText();
			facilityId = main.get("facilityId").asText();
			locale = main.get("locale").asText();
		} catch (Exception e)
		{
			throw new IllegalArgumentException("Can't perform JSON deserialization", e);
		}

	}
}
