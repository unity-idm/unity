/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.confirmations.facilities;

import org.apache.ibatis.session.SqlSession;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.confirmations.ConfirmationRedirectURLBuilder.ConfirmedElementType;
import pl.edu.icm.unity.confirmations.ConfirmationStatus;
import pl.edu.icm.unity.confirmations.states.RegistrationConfirmationState;
import pl.edu.icm.unity.confirmations.states.RegistrationConfirmationState.RequestType;
import pl.edu.icm.unity.db.generic.reg.EnquiryFormDB;
import pl.edu.icm.unity.db.generic.reg.EnquiryResponseDB;
import pl.edu.icm.unity.db.generic.reg.RegistrationFormDB;
import pl.edu.icm.unity.db.generic.reg.RegistrationRequestDB;
import pl.edu.icm.unity.engine.registration.SharedEnquiryManagment;
import pl.edu.icm.unity.engine.registration.SharedRegistrationManagment;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.registration.RegistrationRedirectURLBuilder;
import pl.edu.icm.unity.server.api.registration.RegistrationRedirectURLBuilder.Status;
import pl.edu.icm.unity.types.registration.BaseRegistrationInput;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryResponseState;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.types.registration.UserRequestState;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Common code for processing verifiable elements for elements existing in registration (as opposed to 
 * elements bound to existing entities).
 * 
 * @author K. Benedyczak
 */
public abstract class RegistrationFacility <T extends RegistrationConfirmationState> extends BaseFacility<T>
{
	protected final ObjectMapper mapper = Constants.MAPPER;
	
	protected RegistrationRequestDB requestDB;
	protected EnquiryResponseDB enquiryResponsesDB;
	protected RegistrationFormDB formsDB;
	protected EnquiryFormDB enquiresDB;
	protected SharedRegistrationManagment internalRegistrationManagment;
	protected SharedEnquiryManagment internalEnquiryManagment;

	public RegistrationFacility(RegistrationRequestDB requestDB, EnquiryResponseDB enquiryResponsesDB,
			RegistrationFormDB formsDB, EnquiryFormDB enquiresDB,
			SharedRegistrationManagment internalRegistrationManagment,
			SharedEnquiryManagment internalEnquiryManagment)
	{
		this.requestDB = requestDB;
		this.enquiryResponsesDB = enquiryResponsesDB;
		this.formsDB = formsDB;
		this.enquiresDB = enquiresDB;
		this.internalRegistrationManagment = internalRegistrationManagment;
		this.internalEnquiryManagment = internalEnquiryManagment;
	}

	@Override
	public boolean isDuplicate(T base, String candidate)
	{
		ObjectNode main = JsonUtil.parse(candidate);
		if (!main.has("requestId"))
			return false;
		String requestId = main.get("requestId").asText();
		String value = main.get("value").asText();
		return base.getRequestId().equals(requestId) && base.getValue().equals(value);
	}
	
	protected abstract ConfirmationStatus confirmElements(UserRequestState<?> reqState, T state) 
			throws EngineException;
	
	protected abstract ConfirmedElementType getConfirmedElementType(T state);

	protected String getSuccessRedirect(T state, UserRequestState<?> reqState)
	{
		return new RegistrationRedirectURLBuilder(state.getRedirectUrl(), reqState.getRequest().getFormId(),
				reqState.getRequestId(), Status.elementConfirmed).
			setConfirmationInfo(getConfirmedElementType(state), state.getType(), state.getValue()).
			build();
	}
	
	protected String getErrorRedirect(T state, UserRequestState<?> reqState)
	{
		return new RegistrationRedirectURLBuilder(state.getRedirectUrl(), reqState.getRequest().getFormId(),
				reqState.getRequestId(), Status.elementConfirmationError).
			setConfirmationInfo(getConfirmedElementType(state), state.getType(), state.getValue()).
			build();
	}
	
	@Override
	public ConfirmationStatus processConfirmation(String rawState, SqlSession sql) throws EngineException
	{
		T state = parseState(rawState);
		String requestId = state.getRequestId();

		UserRequestState<?> reqState = null;
		try
		{
			reqState = state.getRequestType() == RequestType.REGISTRATION ? 
					requestDB.get(requestId, sql) : enquiryResponsesDB.get(requestId, sql);
		} catch (EngineException e)
		{
			String redirect = new RegistrationRedirectURLBuilder(state.getRedirectUrl(), null,
					requestId, Status.elementConfirmationError).
				setErrorCode("requestDeleted").
				setConfirmationInfo(getConfirmedElementType(state), state.getType(), state.getValue()).
				build();
			return new ConfirmationStatus(false, redirect, "ConfirmationStatus.requestDeleted");
		}
		
		if (reqState.getStatus().equals(RegistrationRequestStatus.rejected))
		{
			String redirect = new RegistrationRedirectURLBuilder(state.getRedirectUrl(), null,
					requestId, Status.elementConfirmationError).
				setErrorCode("requestRejected").
				setConfirmationInfo(getConfirmedElementType(state), state.getType(), state.getValue()).
				build();
			return new ConfirmationStatus(false, redirect, "ConfirmationStatus.requestRejected");
		}
		BaseRegistrationInput req = reqState.getRequest();
		ConfirmationStatus status = confirmElements(reqState, state);
		
		if (state.getRequestType() == RequestType.REGISTRATION)
			requestDB.update(requestId, (RegistrationRequestState) reqState, sql);
		else
			enquiryResponsesDB.update(requestId, (EnquiryResponseState) reqState, sql);
		//make sure we update request, later on auto-acceptance may fail
		sql.commit();
		
		if (status.isSuccess() && reqState.getStatus().equals(RegistrationRequestStatus.pending))
		{
			if (state.getRequestType() == RequestType.REGISTRATION)
			{
				RegistrationForm form = formsDB.get(req.getFormId(), sql);
				String info = "Automatically processing registration request " + state.getRequestId()
					+ " after confirmation [" + state.getType() + "]" + state.getValue() + " by "
					+ state.getFacilityId() + ". Action: {0}";
			
				internalRegistrationManagment.autoProcess(form, 
						(RegistrationRequestState) reqState, info, sql);
			} else
			{
				String info = "Automatically processing enquiry response " + state.getRequestId()
						+ " after confirmation [" + state.getType() + "]" + state.getValue() + " by "
						+ state.getFacilityId() + ". Action: {0}";
				EnquiryForm form = enquiresDB.get(req.getFormId(), sql);
				internalEnquiryManagment.autoProcessEnquiry(form, 
						(EnquiryResponseState) reqState, info, sql);
			}
		}

		return status;
	}
}
