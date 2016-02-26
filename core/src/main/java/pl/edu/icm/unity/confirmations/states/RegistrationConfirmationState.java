/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.confirmations.states;

import pl.edu.icm.unity.exceptions.WrongArgumentException;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Base class for states of confirmation process which are bound to a registration request (as opposite 
 * to confirmations associated with an existing user).
 * @author K. Benedyczak
 */
public class RegistrationConfirmationState extends BaseConfirmationState
{
	public enum RequestType {REGISTRATION, ENQUIRY}
	
	protected String requestId;
	protected RequestType requestType;
	
	
	public RegistrationConfirmationState(String facilityId, String type, String value, String locale,
			String requestId, RequestType requestType)
	{
		super(facilityId, type, value, locale);
		this.requestId = requestId;
		this.requestType = requestType;
	}


	public RegistrationConfirmationState(String facilityId, String type, String value, String locale,
			String redirectUrl, String requestId, RequestType requestType)
	{
		super(facilityId, type, value, locale, redirectUrl);
		this.requestId = requestId;
		this.requestType = requestType;
	}


	public RegistrationConfirmationState(String serializedState) throws WrongArgumentException
	{
		super();
		setSerializedConfiguration(serializedState);
	}

	protected RegistrationConfirmationState()
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
	protected void setSerializedConfiguration(ObjectNode main) throws WrongArgumentException
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
			throw new WrongArgumentException("Can't perform JSON deserialization", e);
		}
	}
}
