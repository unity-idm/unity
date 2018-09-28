/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.confirmation.states;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.JsonUtil;

/**
 * Contains necessary informations used during the confirmation a attribute in
 * registration request
 * 
 * @author P. Piernik
 * 
 */
public class RegistrationReqEmailAttribiuteConfirmationState extends RegistrationEmailConfirmationState
{
	public static final String FACILITY_ID = "RegistrationReqAttributeFacility";
	
	private String group;
	
	public RegistrationReqEmailAttribiuteConfirmationState(String serializedState)
	{
		super();
		setSerializedConfiguration(serializedState);
	}

	public RegistrationReqEmailAttribiuteConfirmationState(String requestId,
			String type, String value, String locale, String group, RequestType requestType)
	{
		super(FACILITY_ID, type, value, locale, requestId, requestType);
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
	
	protected void setSerializedConfiguration(String json)
	{
		ObjectNode main = JsonUtil.parse(json);
		super.setSerializedConfiguration(main);
		try
		{
			group = main.get("group").asText();	
		} catch (Exception e)
		{
			throw new IllegalArgumentException("Can't perform JSON deserialization", e);
		}
	}
}
