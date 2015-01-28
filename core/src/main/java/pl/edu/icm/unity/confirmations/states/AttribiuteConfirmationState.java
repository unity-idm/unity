/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.confirmations.states;

import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.utils.JsonUtil;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Contains necessary informations used during the confirmation a attribute
 * 
 * @author P. Piernik
 * 
 */
public class AttribiuteConfirmationState extends UserConfirmationState
{
	public static final String FACILITY_ID = "AttributeFacility";
	private String group;

	
	public AttribiuteConfirmationState(long owner, String type,
			String value, String locale, String group)
	{
		super(FACILITY_ID, type, value, locale, owner);
		this.group = group;
	}
	
	
	public AttribiuteConfirmationState(long owner, String type,
			String value, String locale, String group, String successUrl, String errorUrl)
	{
		super(FACILITY_ID, type, value, locale, successUrl, errorUrl, owner);
		this.group = group;
	}

	public AttribiuteConfirmationState(String serializedState) throws WrongArgumentException
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
