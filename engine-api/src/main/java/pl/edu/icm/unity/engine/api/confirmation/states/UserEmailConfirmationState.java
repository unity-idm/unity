/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.confirmation.states;

import pl.edu.icm.unity.exceptions.WrongArgumentException;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Base class for states of confirmation process which are bound to a user existing in the database (as opposite 
 * to confirmations associated with a registration request).
 * @author K. Benedyczak
 */
public class UserEmailConfirmationState extends BaseEmailConfirmationState
{
	protected long ownerEntityId;
	
	
	public UserEmailConfirmationState(String facilityId, String type, String value, String locale,
			long ownerEntityId)
	{
		super(facilityId, type, value, locale);
		this.ownerEntityId = ownerEntityId;
	}

	public UserEmailConfirmationState(String serializedState) throws WrongArgumentException
	{
		super();
		setSerializedConfiguration(serializedState);
	}

	protected UserEmailConfirmationState()
	{
		super();
	}


	public long getOwnerEntityId()
	{
		return ownerEntityId;
	}

	public void setOwnerEntityId(long ownerEntityId)
	{
		this.ownerEntityId = ownerEntityId;
	}
	
	@Override
	protected ObjectNode createState()
	{
		ObjectNode state = super.createState();
		state.put("ownerEntityId", getOwnerEntityId());
		return state;
	}
	
	@Override
	protected void setSerializedConfiguration(ObjectNode main)
	{
		super.setSerializedConfiguration(main);
		try
		{
			ownerEntityId = main.get("ownerEntityId").asLong();	
		} catch (Exception e)
		{
			throw new IllegalArgumentException("Can't perform JSON deserialization", e);
		}

	}
}
