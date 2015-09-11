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

import pl.edu.icm.unity.confirmations.ConfirmationRedirectURLBuilder.ConfirmedElementType;
import pl.edu.icm.unity.confirmations.ConfirmationStatus;
import pl.edu.icm.unity.confirmations.states.IdentityConfirmationState;
import pl.edu.icm.unity.db.DBIdentities;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;

/**
 * Identity confirmation facility.
 * 
 * @author P. Piernik
 */
@Component
public class IdentityFacility extends UserFacility<IdentityConfirmationState>
{
	@Autowired
	protected IdentityFacility(DBSessionManager db, DBIdentities dbIdentities)
	{
		super(db, dbIdentities);
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
	protected ConfirmationStatus confirmElements(IdentityConfirmationState idState, SqlSession sql) 
			throws EngineException
	{
		ConfirmationStatus status;
		Identity[] ids = dbIdentities.getIdentitiesForEntityNoContext(
				idState.getOwnerEntityId(), sql);

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
				confirmed ? getSuccessRedirect(idState) : getErrorRedirect(idState),
				confirmed ? "ConfirmationStatus.successIdentity"
						: "ConfirmationStatus.identityChanged",
						idState.getType());
		return status;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void processAfterSendRequest(String state) throws EngineException
	{
		IdentityConfirmationState idState = new IdentityConfirmationState(state);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			Identity[] ids = dbIdentities.getIdentitiesForEntityNoContext(
					idState.getOwnerEntityId(), sql);
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

	@Override
	public IdentityConfirmationState parseState(String state) throws WrongArgumentException
	{
		return new IdentityConfirmationState(state);
	}

	@Override
	protected ConfirmedElementType getConfirmedElementType(IdentityConfirmationState state)
	{
		return ConfirmedElementType.identity;
	}
}
