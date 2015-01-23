/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.confirmations.facilities;

import org.apache.ibatis.session.SqlSession;

import pl.edu.icm.unity.confirmations.ConfirmationStatus;
import pl.edu.icm.unity.confirmations.states.BaseConfirmationState;
import pl.edu.icm.unity.db.DBIdentities;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.EntityState;

/**
 * Common code for processing verifiable elements for the entities existing in database (as opposed to 
 * elements existing in registrations).
 * 
 * @author K. Benedyczak
 */
public abstract class UserFacility <T extends BaseConfirmationState> extends BaseFacility
{
	protected DBIdentities dbIdentities;

	protected UserFacility(DBSessionManager db, DBIdentities dbIdentities)
	{
		super(db);
		this.dbIdentities = dbIdentities;
	}

	protected abstract T parseState(String state);
	protected abstract ConfirmationStatus confirmElements(T state) throws EngineException;
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public ConfirmationStatus processConfirmation(String state) throws EngineException
	{
		T idState = parseState(state);
		SqlSession sql = db.getSqlSession(false);
		EntityState entityState = null;
		try
		{
			entityState = dbIdentities.getEntityStatus(
					Long.parseLong(idState.getOwner()), sql);

		} catch (Exception e)
		{
			return new ConfirmationStatus(false, idState.getErrorUrl(), "ConfirmationStatus.entityRemoved");
		} finally
		{
			db.releaseSqlSession(sql);
		}

		if (!entityState.equals(EntityState.valid))
		{
			return new ConfirmationStatus(false, idState.getErrorUrl(), "ConfirmationStatus.entityInvalid");
		}
			
		return confirmElements(idState);
	}
	
}
