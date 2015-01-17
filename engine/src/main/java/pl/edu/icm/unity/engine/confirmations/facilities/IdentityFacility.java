/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.confirmations.facilities;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.confirmations.ConfirmationFacility;
import pl.edu.icm.unity.confirmations.ConfirmationStatus;
import pl.edu.icm.unity.confirmations.states.BaseConfirmationState;
import pl.edu.icm.unity.confirmations.states.IdentityConfirmationState;
import pl.edu.icm.unity.db.DBIdentities;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;

/**
 * Identity confirmation facility.
 * 
 * @author P. Piernik
 */
@Component
public class IdentityFacility extends BaseFacility implements ConfirmationFacility
{
	protected DBSessionManager db;
	protected DBIdentities dbIdentities;

	@Autowired
	protected IdentityFacility(DBSessionManager db, DBIdentities dbIdentities)
	{
		this.db = db;
		this.dbIdentities = dbIdentities;
	}

	@Override
	public String getName()
	{
		return IdentityConfirmationState.FACILITY_ID;
	}

	@Override
	public String getDescription()
	{
		return "Confirms verifiable identity";
	}

	@Override
	public ConfirmationStatus confirm(String state) throws EngineException
	{
		BaseConfirmationState baseState = new BaseConfirmationState();
		baseState.setSerializedConfiguration(state);
		SqlSession sql = db.getSqlSession(false);
		EntityState entityState = null;
		try
		{
			entityState = dbIdentities.getEntityStatus(
					Long.parseLong(baseState.getOwner()), sql);

		} catch (Exception e)
		{
			return new ConfirmationStatus(false, "ConfirmationStatus.entityRemoved");
		} finally
		{
			db.releaseSqlSession(sql);
		}

		if (!entityState.equals(EntityState.valid))
		{
			return new ConfirmationStatus(false, "ConfirmationStatus.entityInvalid");
		}
			
		return confirmElements(state);
	}

	private IdentityConfirmationState getState(String state)
	{
		IdentityConfirmationState idState = new IdentityConfirmationState();
		idState.setSerializedConfiguration(state);
		return idState;
	}

	protected ConfirmationStatus confirmElements(String state) throws EngineException
	{
		IdentityConfirmationState idState = getState(state);
		ConfirmationStatus status;
		SqlSession sql = db.getSqlSession(true);
		try
		{
			Identity[] ids = dbIdentities.getIdentitiesForEntityNoContext(
					Long.parseLong(idState.getOwner()), sql);

			ArrayList<IdentityParam> idsA = new ArrayList<IdentityParam>();
			for (Identity id : ids)
			{
				if (id.getType().getIdentityTypeProvider().isVerifiable())
					idsA.add(id);
			}

			Collection<IdentityParam> confirmedList = confirmIdentity(idsA,
					idState.getType(), idState.getValue());
			for (IdentityParam id : confirmedList)
			{
				dbIdentities.updateIdentityConfirmationInfo(id,
						id.getConfirmationInfo(), sql);
			}
			sql.commit();
			boolean confirmed = (confirmedList.size() > 0);
			status = new ConfirmationStatus(confirmed,
					confirmed ? "ConfirmationStatus.successIdentity"
							: "ConfirmationStatus.identityChanged", idState.getType());

		} finally
		{
			db.releaseSqlSession(sql);
		}
		return status;
	}

	@Override
	public void updateAfterSendRequest(String state) throws EngineException
	{
		IdentityConfirmationState idState = getState(state);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			Identity[] ids = dbIdentities.getIdentitiesForEntityNoContext(
					Long.parseLong(idState.getOwner()), sql);
			for (IdentityParam id : ids)
			{
				updateConfirmationInfo(id, idState.getValue());
				dbIdentities.updateIdentityConfirmationInfo(id, id.getConfirmationInfo(), sql);
			}
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

}
