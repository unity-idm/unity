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
	protected String requestId;
	
	
	public RegistrationConfirmationState(String facilityId, String type, String value, String locale,
			String requestId)
	{
		super(facilityId, type, value, locale);
		this.requestId = requestId;
	}


	public RegistrationConfirmationState(String facilityId, String type, String value, String locale,
			String successUrl, String errorUrl, String requestId)
	{
		super(facilityId, type, value, locale, successUrl, errorUrl);
		this.requestId = requestId;
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


	@Override
	protected ObjectNode createState()
	{
		ObjectNode state = super.createState();
		state.put("requestId", getRequestId());
		return state;
	}
	
	
	@Override
	protected void setSerializedConfiguration(ObjectNode main) throws WrongArgumentException
	{
		super.setSerializedConfiguration(main);
		try
		{
			requestId = main.get("requestId").asText();
		} catch (Exception e)
		{
			throw new WrongArgumentException("Can't perform JSON deserialization", e);
		}
	}
}
