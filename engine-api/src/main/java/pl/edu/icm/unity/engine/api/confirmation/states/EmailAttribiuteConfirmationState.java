/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.confirmation.states;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.JsonUtil;

/**
 * Contains necessary informations used during the confirmation a attribute
 * 
 * @author P. Piernik
 * 
 */
public class EmailAttribiuteConfirmationState extends UserEmailConfirmationState
{
	public static final String FACILITY_ID = "AttributeFacility";
	private String group;

	
	public EmailAttribiuteConfirmationState(long owner, String type,
			String value, String locale, String group)
	{
		super(FACILITY_ID, type, value, locale, owner);
		this.group = group;
	}
	
	public EmailAttribiuteConfirmationState(String serializedState)
	{
		super();
		setSerializedConfiguration(serializedState);
	}

	public String getGroup()
	{
		return group;
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
