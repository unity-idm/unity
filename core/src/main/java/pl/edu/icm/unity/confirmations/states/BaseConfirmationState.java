/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.confirmations.states;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Contains common informations used during confirmation
 * @author P. Piernik
 *
 */
public class BaseConfirmationState
{
	protected final ObjectMapper mapper = Constants.MAPPER;
	
	protected String facilityId;
	protected String type;
	protected String value;
	protected String locale;
	protected String redirectUrl;
	
	public BaseConfirmationState(String facilityId, String type, String value, String locale)
	{
		this.type = type;
		this.value = value;
		this.locale = locale;
		this.facilityId = facilityId;
	}
	
	public BaseConfirmationState(String facilityId, String type, String value,
			String locale, String redirectUrl)
	{
		this(facilityId, type, value, locale);
		this.redirectUrl = redirectUrl;
	}

	public BaseConfirmationState(String serializedState) throws WrongArgumentException
	{
		setSerializedConfiguration(serializedState);
	}

	protected BaseConfirmationState()
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
	public String getRedirectUrl()
	{
		return redirectUrl;
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
		if (getRedirectUrl() != null)
			state.put("redirectUrl", getRedirectUrl());
		return state;
	}
	
	protected void setSerializedConfiguration(String json) throws WrongArgumentException
	{
		setSerializedConfiguration(JsonUtil.parse(json));
	}
	
	protected void setSerializedConfiguration(ObjectNode main) throws WrongArgumentException
	{
		try
		{
			type = main.get("type").asText();
			value = main.get("value").asText();
			facilityId = main.get("facilityId").asText();
			locale = main.get("locale").asText();
			if (main.has("redirectUrl"))
				redirectUrl = main.get("redirectUrl").asText();
		} catch (Exception e)
		{
			throw new WrongArgumentException("Can't perform JSON deserialization", e);
		}

	}
}
