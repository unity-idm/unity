/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.confirmation.facilities;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationRedirectURLBuilder;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationRedirectURLBuilder.ConfirmedElementType;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationRedirectURLBuilder.Status;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationStatus;
import pl.edu.icm.unity.engine.api.confirmation.states.UserEmailConfirmationState;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.types.basic.EntityState;

/**
 * Common code for processing verifiable elements for the entities existing in database (as opposed to 
 * elements existing in registrations).
 * 
 * @author K. Benedyczak
 */
public abstract class UserEmailFacility <T extends UserEmailConfirmationState> extends BaseEmailFacility<T>
{
	protected final ObjectMapper mapper = Constants.MAPPER;
	protected EntityDAO entityDAO;

	protected UserEmailFacility(EntityDAO entityDAO)
	{
		this.entityDAO = entityDAO;
	}

	protected abstract EmailConfirmationStatus confirmElements(T state) throws EngineException;
	
	protected abstract ConfirmedElementType getConfirmedElementType(T state);
	
	protected String getSuccessRedirect(T state)
	{
		return new EmailConfirmationRedirectURLBuilder(state.getRedirectUrl(), 
				Status.elementConfirmed).
			setConfirmationInfo(getConfirmedElementType(state), state.getType(), state.getValue()).
			build();
	}
	
	protected String getErrorRedirect(T state)
	{
		return new EmailConfirmationRedirectURLBuilder(state.getRedirectUrl(), Status.elementConfirmationError).
			setConfirmationInfo(getConfirmedElementType(state), state.getType(), state.getValue()).
			build();
	}
	
	@Override
	public boolean isDuplicate(T base, String candidate)
	{
		ObjectNode main;
		try
		{
			main = mapper.readValue(candidate, ObjectNode.class);
		} catch (Exception e)
		{
			throw new InternalException("Can't perform JSON deserialization", e);
		}
		if (!main.has("ownerEntityId"))
			return false;
		long ownerEntityId = main.get("ownerEntityId").asLong();
		String value = main.get("value").asText();
		return base.getOwnerEntityId() == ownerEntityId && base.getValue().equals(value);
	}
	
	@Override
	public EmailConfirmationStatus processConfirmation(String state) throws EngineException
	{
		T idState = parseState(state);
		EntityState entityState = null;
		try
		{
			entityState = entityDAO.getByKey(idState.getOwnerEntityId()).getEntityState();
		} catch (Exception e)
		{
			return new EmailConfirmationStatus(false, idState.getRedirectUrl(), 
					"ConfirmationStatus.entityRemoved");
		}

		if (!entityState.equals(EntityState.valid))
		{
			return new EmailConfirmationStatus(false, idState.getRedirectUrl(), 
					"ConfirmationStatus.entityInvalid");
		}
			
		return confirmElements(idState);
	}
	
}
