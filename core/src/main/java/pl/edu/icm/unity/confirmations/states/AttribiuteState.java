/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.confirmations.states;

import pl.edu.icm.unity.exceptions.InternalException;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Contains necessary informations used during the confirmation a attribute
 * 
 * @author P. Piernik
 * 
 */
public class AttribiuteState extends IdentityConfirmationState
{
	public static final String FACILITY_ID = "attributeFacility";
	private String group;

	@Override
	public String getFacilityId()
	{
		return FACILITY_ID;
	}

	public String getGroup()
	{
		return group;
	}

	public void setGroup(String group)
	{
		this.group = group;
	}

	@Override
	protected ObjectNode createState()
	{
		ObjectNode state = super.createState();
		state.put("group", getGroup());
		return state;
	}
	
	@Override
	public void setSerializedConfiguration(String json) throws InternalException
	{
		super.setSerializedConfiguration(json);
		try
		{
			ObjectNode main = mapper.readValue(json, ObjectNode.class);
			setGroup(main.get("group").asText());	
		} catch (Exception e)
		{
			throw new InternalException("Can't perform JSON deserialization", e);
		}
	}
}
