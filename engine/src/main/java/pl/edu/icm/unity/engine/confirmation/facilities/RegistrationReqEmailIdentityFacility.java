/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.confirmation.facilities;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationRedirectURLBuilder.ConfirmedElementType;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationStatus;
import pl.edu.icm.unity.engine.api.confirmation.states.RegistrationEmailConfirmationState.RequestType;
import pl.edu.icm.unity.engine.api.confirmation.states.RegistrationReqEmailIdentityConfirmationState;
import pl.edu.icm.unity.engine.api.identity.IdentityTypesRegistry;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.store.api.generic.EnquiryFormDB;
import pl.edu.icm.unity.store.api.generic.EnquiryResponseDB;
import pl.edu.icm.unity.store.api.generic.RegistrationFormDB;
import pl.edu.icm.unity.store.api.generic.RegistrationRequestDB;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.store.api.tx.TxManager;
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
public class RegistrationReqEmailIdentityFacility extends RegistrationEmailFacility<RegistrationReqEmailIdentityConfirmationState>
{
	private IdentityTypesRegistry identityTypesRegistry;

	@Autowired
	public RegistrationReqEmailIdentityFacility(
			RegistrationRequestDB requestDB, EnquiryResponseDB enquiryResponsesDB, 
			RegistrationFormDB formsDB, EnquiryFormDB enquiresDB,
			ApplicationEventPublisher publisher,
			IdentityTypesRegistry identityTypesRegistry,
			TxManager tx)
	{
		super(requestDB, enquiryResponsesDB, formsDB, enquiresDB, publisher, tx);
		this.identityTypesRegistry = identityTypesRegistry;
	}

	@Override
	public String getName()
	{
		return RegistrationReqEmailIdentityConfirmationState.FACILITY_ID;
	}

	@Override
	public String getDescription()
	{
		return "Confirms verifiable identity from registration request";
	}

	@Override
	protected EmailConfirmationStatus confirmElements(UserRequestState<?> reqState,
			RegistrationReqEmailIdentityConfirmationState idState) throws EngineException
	{
		if (!(identityTypesRegistry.getByName(idState.getType()).isEmailVerifiable()))
			return new EmailConfirmationStatus(false, "ConfirmationStatus.identityChanged", idState.getType());
		Collection<IdentityParam> confirmedList = confirmIdentity(reqState.getRequest().getIdentities(),
				idState.getType(), idState.getValue());
		boolean confirmed = (confirmedList.size() > 0);
		
		return new EmailConfirmationStatus(confirmed, confirmed ? getSuccessRedirect(idState, reqState)
				: getErrorRedirect(idState, reqState),
				confirmed ? "ConfirmationStatus.successIdentity"
						: "ConfirmationStatus.identityChanged",
				idState.getType());
	}

	@Override
	@Transactional
	public void processAfterSendRequest(String state) throws EngineException
	{
		RegistrationReqEmailIdentityConfirmationState idState = new RegistrationReqEmailIdentityConfirmationState(state);
		String requestId = idState.getRequestId();

		UserRequestState<?> reqState = idState.getRequestType() == RequestType.REGISTRATION ?
				requestDB.get(requestId) : enquiryResponsesDB.get(requestId);
		for (IdentityParam id : reqState.getRequest().getIdentities())
		{
			if (id == null)
				continue;
			if (identityTypesRegistry.getByName(id.getTypeId()).isEmailVerifiable())
				updateConfirmationInfo(id, id.getValue());
		}
		if (idState.getRequestType() == RequestType.REGISTRATION)
			requestDB.update((RegistrationRequestState) reqState);
		else
			enquiryResponsesDB.update((EnquiryResponseState) reqState);
	}

	@Override
	public RegistrationReqEmailIdentityConfirmationState parseState(String state)
	{
		return new RegistrationReqEmailIdentityConfirmationState(state);
	}

	@Override
	protected ConfirmedElementType getConfirmedElementType(
			RegistrationReqEmailIdentityConfirmationState state)
	{
		return ConfirmedElementType.identity;
	}
}
