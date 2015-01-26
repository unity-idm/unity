/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.confirmations.states;

import pl.edu.icm.unity.exceptions.InternalException;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Contains necessary informations used during the confirmation a attribute in
 * registration request
 * 
 * @author P. Piernik
 * 
 */
public class RegistrationReqAttribiuteConfirmationState extends RegistrationConfirmationState
{
	public static final String FACILITY_ID = "RegistrationReqAttributeFacility";
	
	private String group;
	
	public RegistrationReqAttribiuteConfirmationState(String serializedState)
	{
		super();
		setSerializedConfiguration(serializedState);
	}

	public RegistrationReqAttribiuteConfirmationState(String requestId,
			String type, String value, String locale, String group, String successUrl, String errorUrl)
	{
		super(FACILITY_ID, type, value, locale, successUrl, errorUrl, requestId);
		this.group = group;
	}
	
	public RegistrationReqAttribiuteConfirmationState(String requestId,
			String type, String value, String locale, String group)
	{
		super(FACILITY_ID, type, value, locale, requestId);
		this.group = group;
	}

	public String getGroup()
	{
		return group;
	}

	@Override
	public String getFacilityId()
	{
		return FACILITY_ID;
	}
	
	@Override
	protected ObjectNode createState()
	{
		ObjectNode state = super.createState();
		state.put("group", getGroup());
		return state;
	}
	
	protected void setSerializedConfiguration(String json) throws InternalException
	{
		try
		{
			ObjectNode main = mapper.readValue(json, ObjectNode.class);
			super.setSerializedConfiguration(main);
			group = main.get("group").asText();	
		} catch (Exception e)
		{
			throw new InternalException("Can't perform JSON deserialization", e);
		}
	}
}
