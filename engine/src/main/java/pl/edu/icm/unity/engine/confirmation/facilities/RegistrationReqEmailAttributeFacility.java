/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.confirmation.facilities;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationRedirectURLBuilder.ConfirmedElementType;
import pl.edu.icm.unity.engine.api.confirmation.states.RegistrationEmailConfirmationState.RequestType;
import pl.edu.icm.unity.engine.api.confirmation.states.RegistrationReqEmailAttribiuteConfirmationState;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.attribute.AttributeTypeHelper;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.store.api.generic.EnquiryFormDB;
import pl.edu.icm.unity.store.api.generic.EnquiryResponseDB;
import pl.edu.icm.unity.store.api.generic.RegistrationFormDB;
import pl.edu.icm.unity.store.api.generic.RegistrationRequestDB;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.store.api.tx.TxManager;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.registration.EnquiryResponseState;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.UserRequestState;

/**
 * Attribute from registration request confirmation facility.
 * 
 * @author P. Piernik
 * 
 */
@Component
public class RegistrationReqEmailAttributeFacility extends RegistrationEmailFacility<RegistrationReqEmailAttribiuteConfirmationState>
{
	public static final String NAME = "registrationRequestVerificator";
	private AttributeTypeHelper atHelper;

	@Autowired
	public RegistrationReqEmailAttributeFacility(RegistrationRequestDB requestDB, EnquiryResponseDB enquiryResponsesDB, 
			RegistrationFormDB formsDB, EnquiryFormDB enquiresDB,
			ApplicationEventPublisher publisher,
			AttributeTypeHelper atHelper,
			TxManager txMan, UnityMessageSource msg)
	{
		super(requestDB, enquiryResponsesDB, formsDB, enquiresDB, publisher, txMan, msg);
		this.atHelper = atHelper;
	}

	@Override
	public String getName()
	{
		return RegistrationReqEmailAttribiuteConfirmationState.FACILITY_ID;
	}

	@Override
	public String getDescription()
	{
		return "Confirms attributes from registration request with verifiable values";
	}

	@Override
	protected boolean confirmElements(UserRequestState<?> reqState, 
			RegistrationReqEmailAttribiuteConfirmationState attrState)
	{
		
		Collection<Attribute> confirmedList = confirmAttributes(reqState.getRequest().getAttributes(),
				attrState.getType(), attrState.getGroup(), attrState.getValue(), atHelper);
		return (confirmedList.size() > 0);
	}
	
	@Override
	@Transactional
	public void processAfterSendRequest(String state) throws EngineException
	{
		RegistrationReqEmailAttribiuteConfirmationState attrState = 
				new RegistrationReqEmailAttribiuteConfirmationState(state);
		String requestId = attrState.getRequestId();
		UserRequestState<?> reqState = attrState.getRequestType() == RequestType.REGISTRATION ?
				requestDB.get(requestId) : enquiryResponsesDB.get(requestId);
		for (Attribute attr : reqState.getRequest().getAttributes())
		{
			if (attr == null)
				continue;
			AttributeValueSyntax<?> syntax = atHelper.getUnconfiguredSyntax(attr.getValueSyntax());
			if (syntax.isEmailVerifiable())
				updateConfirmationForAttributeValues(attr.getValues(), syntax, attrState.getValue());
		}
		
		if (attrState.getRequestType() == RequestType.REGISTRATION)
			requestDB.update((RegistrationRequestState) reqState);
		else
			enquiryResponsesDB.update((EnquiryResponseState) reqState);
	}

	@Override
	public RegistrationReqEmailAttribiuteConfirmationState parseState(String state)
	{
		return new RegistrationReqEmailAttribiuteConfirmationState(state);
	}

	@Override
	protected ConfirmedElementType getConfirmedElementType(
			RegistrationReqEmailAttribiuteConfirmationState state)
	{
		return ConfirmedElementType.attribute;
	}
}
