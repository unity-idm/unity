/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.confirmations.facilities;

import java.util.Collection;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.confirmations.ConfirmationStatus;
import pl.edu.icm.unity.confirmations.states.RegistrationReqIdentityConfirmationState;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.generic.reg.RegistrationFormDB;
import pl.edu.icm.unity.db.generic.reg.RegistrationRequestDB;
import pl.edu.icm.unity.engine.internal.InternalRegistrationManagment;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.registries.IdentityTypesRegistry;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;

/**
 * Identity from registration confirmation facility.
 * 
 * @author P. Piernik
 */
@Component
public class RegistrationReqIdentityFacility extends RegistrationFacility<RegistrationReqIdentityConfirmationState>
{
	private IdentityTypesRegistry identityTypesRegistry;

	@Autowired
	public RegistrationReqIdentityFacility(DBSessionManager db,
			RegistrationRequestDB requestDB, RegistrationFormDB formsDB,
			InternalRegistrationManagment internalRegistrationManagment,
			IdentityTypesRegistry identityTypesRegistry)
	{
		super(db, requestDB, formsDB, internalRegistrationManagment);
		this.identityTypesRegistry = identityTypesRegistry;
	}

	@Override
	public String getName()
	{
		return RegistrationReqIdentityConfirmationState.FACILITY_ID;
	}

	@Override
	public String getDescription()
	{
		return "Confirms verifiable identity from registration request";
	}

	@Override
	protected ConfirmationStatus confirmElements(RegistrationRequest req,
			RegistrationReqIdentityConfirmationState idState) throws EngineException
	{
		if (!(identityTypesRegistry.getByName(idState.getType()).isVerifiable()))
			return new ConfirmationStatus(false, "ConfirmationStatus.identityChanged", idState.getType());
		Collection<IdentityParam> confirmedList = confirmIdentity(req.getIdentities(),
				idState.getType(), idState.getValue());
		boolean confirmed = (confirmedList.size() > 0);
		return new ConfirmationStatus(confirmed, confirmed ? idState.getSuccessUrl()
				: idState.getErrorUrl(),
				confirmed ? "ConfirmationStatus.successIdentity"
						: "ConfirmationStatus.identityChanged",
				idState.getType());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void processAfterSendRequest(String state) throws EngineException
	{
		RegistrationReqIdentityConfirmationState idState = new RegistrationReqIdentityConfirmationState(state);
		String requestId = idState.getRequestId();
		SqlSession sql = db.getSqlSession(true);
		try
		{
			RegistrationRequestState reqState = internalRegistrationManagment
					.getRequest(requestId, sql);
			for (IdentityParam id : reqState.getRequest().getIdentities())
			{
				if (id == null || id.getTypeId() == null)
					continue;
				if (identityTypesRegistry.getByName(id.getTypeId()).isVerifiable())
					updateConfirmationInfo(id, id.getValue());
			}
			requestDB.update(requestId, reqState, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	@Override
	public RegistrationReqIdentityConfirmationState parseState(String state) throws WrongArgumentException
	{
		return new RegistrationReqIdentityConfirmationState(state);
	}
}
