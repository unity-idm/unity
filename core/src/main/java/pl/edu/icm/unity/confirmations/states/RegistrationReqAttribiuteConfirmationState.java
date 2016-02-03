/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.confirmations.states;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.exceptions.WrongArgumentException;

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
	
	public RegistrationReqAttribiuteConfirmationState(String serializedState) throws WrongArgumentException
	{
		super();
		setSerializedConfiguration(serializedState);
	}

	public RegistrationReqAttribiuteConfirmationState(String requestId,
			String type, String value, String locale, String group, String redirectUrl)
	{
		super(FACILITY_ID, type, value, locale, redirectUrl, requestId);
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
	
	protected void setSerializedConfiguration(String json) throws WrongArgumentException
	{
		ObjectNode main = JsonUtil.parse(json);
		super.setSerializedConfiguration(main);
		try
		{
			group = main.get("group").asText();	
		} catch (Exception e)
		{
			throw new WrongArgumentException("Can't perform JSON deserialization", e);
		}
	}
}
