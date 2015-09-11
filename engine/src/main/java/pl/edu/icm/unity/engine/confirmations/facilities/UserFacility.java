/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.confirmations.facilities;

import org.apache.ibatis.session.SqlSession;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.confirmations.ConfirmationRedirectURLBuilder;
import pl.edu.icm.unity.confirmations.ConfirmationRedirectURLBuilder.ConfirmedElementType;
import pl.edu.icm.unity.confirmations.ConfirmationRedirectURLBuilder.Status;
import pl.edu.icm.unity.confirmations.ConfirmationStatus;
import pl.edu.icm.unity.confirmations.states.UserConfirmationState;
import pl.edu.icm.unity.db.DBIdentities;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.EntityState;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Common code for processing verifiable elements for the entities existing in database (as opposed to 
 * elements existing in registrations).
 * 
 * @author K. Benedyczak
 */
public abstract class UserFacility <T extends UserConfirmationState> extends BaseFacility<T>
{
	protected final ObjectMapper mapper = Constants.MAPPER;
	protected DBIdentities dbIdentities;

	protected UserFacility(DBSessionManager db, DBIdentities dbIdentities)
	{
		super(db);
		this.dbIdentities = dbIdentities;
	}

	protected abstract ConfirmationStatus confirmElements(T state, SqlSession sql) throws EngineException;
	
	protected abstract ConfirmedElementType getConfirmedElementType(T state);
	
	protected String getSuccessRedirect(T state)
	{
		return new ConfirmationRedirectURLBuilder(state.getRedirectUrl(), 
				Status.elementConfirmed).
			setConfirmationInfo(getConfirmedElementType(state), state.getType(), state.getValue()).
			toString();
	}
	
	protected String getErrorRedirect(T state)
	{
		return new ConfirmationRedirectURLBuilder(state.getRedirectUrl(), Status.elementConfirmationError).
			setConfirmationInfo(getConfirmedElementType(state), state.getType(), state.getValue()).
			toString();
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
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public ConfirmationStatus processConfirmation(String state, SqlSession sql) throws EngineException
	{
		T idState = parseState(state);
		EntityState entityState = null;
		try
		{
			entityState = dbIdentities.getEntityStatus(idState.getOwnerEntityId(), sql);
		} catch (Exception e)
		{
			return new ConfirmationStatus(false, idState.getRedirectUrl(), 
					"ConfirmationStatus.entityRemoved");
		}

		if (!entityState.equals(EntityState.valid))
		{
			return new ConfirmationStatus(false, idState.getRedirectUrl(), 
					"ConfirmationStatus.entityInvalid");
		}
			
		return confirmElements(idState, sql);
	}
	
}
