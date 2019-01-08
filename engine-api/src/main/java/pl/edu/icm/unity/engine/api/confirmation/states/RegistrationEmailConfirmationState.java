/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.confirmation.states;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Base class for states of confirmation process which are bound to a registration request (as opposite 
 * to confirmations associated with an existing user).
 * @author K. Benedyczak
 */
public class RegistrationEmailConfirmationState extends BaseEmailConfirmationState
{
	public enum RequestType {REGISTRATION, ENQUIRY}
	
	protected String requestId;
	protected RequestType requestType;
	
	
	public RegistrationEmailConfirmationState(String facilityId, String type, String value, String locale,
			String requestId, RequestType requestType)
	{
		super(facilityId, type, value, locale);
		this.requestId = requestId;
		this.requestType = requestType;
	}

	public RegistrationEmailConfirmationState(String serializedState)
	{
		super();
		setSerializedConfiguration(serializedState);
	}

	protected RegistrationEmailConfirmationState()
	{
		super();
	}

	public String getRequestId()
	{
		return requestId;
	}


	public void setRequestId(String requestId)
	{
		this.requestId = requestId;
	}

	public RequestType getRequestType()
	{
		return requestType;
	}


	public void setRequestType(RequestType requestType)
	{
		this.requestType = requestType;
	}

	@Override
	protected ObjectNode createState()
	{
		ObjectNode state = super.createState();
		state.put("requestId", getRequestId());
		state.put("requestType", getRequestType().name());
		return state;
	}
	
	
	@Override
	protected void setSerializedConfiguration(ObjectNode main)
	{
		super.setSerializedConfiguration(main);
		try
		{
			requestId = main.get("requestId").asText();
			requestType = main.has("requestType") ?
					RequestType.valueOf(main.get("requestType").asText()) : 
					RequestType.REGISTRATION;
				
		} catch (Exception e)
		{
			throw new IllegalArgumentException("Can't perform JSON deserialization", e);
		}
	}
}
