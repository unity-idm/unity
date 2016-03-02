/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.confirmations.facilities;

import java.util.Collection;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.confirmations.ConfirmationRedirectURLBuilder.ConfirmedElementType;
import pl.edu.icm.unity.confirmations.ConfirmationStatus;
import pl.edu.icm.unity.confirmations.states.RegistrationReqIdentityConfirmationState;
import pl.edu.icm.unity.confirmations.states.RegistrationConfirmationState.RequestType;
import pl.edu.icm.unity.db.generic.reg.EnquiryFormDB;
import pl.edu.icm.unity.db.generic.reg.EnquiryResponseDB;
import pl.edu.icm.unity.db.generic.reg.RegistrationFormDB;
import pl.edu.icm.unity.db.generic.reg.RegistrationRequestDB;
import pl.edu.icm.unity.engine.registration.SharedEnquiryManagment;
import pl.edu.icm.unity.engine.registration.SharedRegistrationManagment;
import pl.edu.icm.unity.engine.transactions.SqlSessionTL;
import pl.edu.icm.unity.engine.transactions.Transactional;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.registries.IdentityTypesRegistry;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.EnquiryResponseState;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.UserRequestState;

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
	public RegistrationReqIdentityFacility(
			RegistrationRequestDB requestDB, EnquiryResponseDB enquiryResponsesDB, 
			RegistrationFormDB formsDB, EnquiryFormDB enquiresDB,
			SharedRegistrationManagment internalRegistrationManagment,
			IdentityTypesRegistry identityTypesRegistry,
			SharedEnquiryManagment internalEnquiryManagment)
	{
		super(requestDB, enquiryResponsesDB, formsDB, enquiresDB, internalRegistrationManagment,
				internalEnquiryManagment);
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
	protected ConfirmationStatus confirmElements(UserRequestState<?> reqState,
			RegistrationReqIdentityConfirmationState idState) throws EngineException
	{
		if (!(identityTypesRegistry.getByName(idState.getType()).isVerifiable()))
			return new ConfirmationStatus(false, "ConfirmationStatus.identityChanged", idState.getType());
		Collection<IdentityParam> confirmedList = confirmIdentity(reqState.getRequest().getIdentities(),
				idState.getType(), idState.getValue());
		boolean confirmed = (confirmedList.size() > 0);
		
		return new ConfirmationStatus(confirmed, confirmed ? getSuccessRedirect(idState, reqState)
				: getErrorRedirect(idState, reqState),
				confirmed ? "ConfirmationStatus.successIdentity"
						: "ConfirmationStatus.identityChanged",
				idState.getType());
	}

	@Override
	@Transactional
	public void processAfterSendRequest(String state) throws EngineException
	{
		RegistrationReqIdentityConfirmationState idState = new RegistrationReqIdentityConfirmationState(state);
		String requestId = idState.getRequestId();
		SqlSession sql = SqlSessionTL.get();

		UserRequestState<?> reqState = idState.getRequestType() == RequestType.REGISTRATION ?
				requestDB.get(requestId, sql) : enquiryResponsesDB.get(requestId, sql);
		for (IdentityParam id : reqState.getRequest().getIdentities())
		{
			if (id == null)
				continue;
			if (identityTypesRegistry.getByName(id.getTypeId()).isVerifiable())
				updateConfirmationInfo(id, id.getValue());
		}
		if (idState.getRequestType() == RequestType.REGISTRATION)
			requestDB.update(requestId, (RegistrationRequestState) reqState, sql);
		else
			enquiryResponsesDB.update(requestId, (EnquiryResponseState) reqState, sql);
	}

	@Override
	public RegistrationReqIdentityConfirmationState parseState(String state) throws WrongArgumentException
	{
		return new RegistrationReqIdentityConfirmationState(state);
	}

	@Override
	protected ConfirmedElementType getConfirmedElementType(
			RegistrationReqIdentityConfirmationState state)
	{
		return ConfirmedElementType.identity;
	}
}
