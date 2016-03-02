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
import pl.edu.icm.unity.confirmations.states.RegistrationConfirmationState.RequestType;
import pl.edu.icm.unity.confirmations.states.RegistrationReqAttribiuteConfirmationState;
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
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.confirmation.VerifiableElement;
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
public class RegistrationReqAttributeFacility extends RegistrationFacility<RegistrationReqAttribiuteConfirmationState>
{
	public static final String NAME = "registrationRequestVerificator";

	@Autowired
	public RegistrationReqAttributeFacility(RegistrationRequestDB requestDB, EnquiryResponseDB enquiryResponsesDB, 
			RegistrationFormDB formsDB, EnquiryFormDB enquiresDB,
			SharedRegistrationManagment internalRegistrationManagment,
			SharedEnquiryManagment internalEnquiryManagment)
	{
		super(requestDB, enquiryResponsesDB, formsDB, enquiresDB, internalRegistrationManagment,
				internalEnquiryManagment);
	}

	@Override
	public String getName()
	{
		return RegistrationReqAttribiuteConfirmationState.FACILITY_ID;
	}

	@Override
	public String getDescription()
	{
		return "Confirms attributes from registration request with verifiable values";
	}

	@Override
	protected ConfirmationStatus confirmElements(UserRequestState<?> reqState, 
			RegistrationReqAttribiuteConfirmationState attrState) throws EngineException
	{
		Collection<Attribute<?>> confirmedList = confirmAttributes(reqState.getRequest().getAttributes(),
				attrState.getType(), attrState.getGroup(), attrState.getValue());
		boolean confirmed = (confirmedList.size() > 0);
		return new ConfirmationStatus(confirmed, confirmed ? getSuccessRedirect(attrState, reqState)
				: getErrorRedirect(attrState, reqState),
				confirmed ? "ConfirmationStatus.successAttribute"
						: "ConfirmationStatus.attributeChanged",
				attrState.getType());
	}
	
	@Override
	@Transactional
	public void processAfterSendRequest(String state) throws EngineException
	{
		RegistrationReqAttribiuteConfirmationState attrState = 
				new RegistrationReqAttribiuteConfirmationState(state);
		String requestId = attrState.getRequestId();
		SqlSession sql = SqlSessionTL.get();
		UserRequestState<?> reqState = attrState.getRequestType() == RequestType.REGISTRATION ?
				requestDB.get(requestId, sql) : enquiryResponsesDB.get(requestId, sql);
		for (Attribute<?> attr : reqState.getRequest().getAttributes())
		{
			if (attr == null)
				continue;

			if (attr.getAttributeSyntax().isVerifiable())
			{
				for (Object val : attr.getValues())
				{
					updateConfirmationInfo((VerifiableElement) val,
							attrState.getValue());
				}
			}
		}
		
		if (attrState.getRequestType() == RequestType.REGISTRATION)
			requestDB.update(requestId, (RegistrationRequestState) reqState, sql);
		else
			enquiryResponsesDB.update(requestId, (EnquiryResponseState) reqState, sql);;
	}

	@Override
	public RegistrationReqAttribiuteConfirmationState parseState(String state) throws WrongArgumentException
	{
		return new RegistrationReqAttribiuteConfirmationState(state);
	}

	@Override
	protected ConfirmedElementType getConfirmedElementType(
			RegistrationReqAttribiuteConfirmationState state)
	{
		return ConfirmedElementType.attribute;
	}
}
