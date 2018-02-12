/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.confirmation.facilities;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationRedirectURLBuilder.ConfirmedElementType;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationStatus;
import pl.edu.icm.unity.engine.api.confirmation.states.RegistrationEmailConfirmationState;
import pl.edu.icm.unity.engine.api.confirmation.states.RegistrationEmailConfirmationState.RequestType;
import pl.edu.icm.unity.engine.api.registration.RegistrationRedirectURLBuilder;
import pl.edu.icm.unity.engine.api.registration.RegistrationRedirectURLBuilder.Status;
import pl.edu.icm.unity.engine.forms.enquiry.SharedEnquiryManagment;
import pl.edu.icm.unity.engine.forms.reg.SharedRegistrationManagment;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.store.api.generic.EnquiryFormDB;
import pl.edu.icm.unity.store.api.generic.EnquiryResponseDB;
import pl.edu.icm.unity.store.api.generic.RegistrationFormDB;
import pl.edu.icm.unity.store.api.generic.RegistrationRequestDB;
import pl.edu.icm.unity.store.api.tx.TxManager;
import pl.edu.icm.unity.types.registration.BaseRegistrationInput;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryResponseState;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.types.registration.UserRequestState;

/**
 * Common code for processing verifiable elements for elements existing in registration (as opposed to 
 * elements bound to existing entities).
 * 
 * @author K. Benedyczak
 */
public abstract class RegistrationEmailFacility <T extends RegistrationEmailConfirmationState> extends BaseEmailFacility<T>
{
	protected final ObjectMapper mapper = Constants.MAPPER;
	
	protected RegistrationRequestDB requestDB;
	protected EnquiryResponseDB enquiryResponsesDB;
	protected RegistrationFormDB formsDB;
	protected EnquiryFormDB enquiresDB;
	protected SharedRegistrationManagment sharedRegistrationManagment;
	protected SharedEnquiryManagment internalEnquiryManagment;

	private TxManager txMan;

	public RegistrationEmailFacility(RegistrationRequestDB requestDB, EnquiryResponseDB enquiryResponsesDB,
			RegistrationFormDB formsDB, EnquiryFormDB enquiresDB,
			SharedRegistrationManagment sharedRegistrationManagment,
			SharedEnquiryManagment internalEnquiryManagment,
			TxManager txMan)
	{
		this.requestDB = requestDB;
		this.enquiryResponsesDB = enquiryResponsesDB;
		this.formsDB = formsDB;
		this.enquiresDB = enquiresDB;
		this.sharedRegistrationManagment = sharedRegistrationManagment;
		this.internalEnquiryManagment = internalEnquiryManagment;
		this.txMan = txMan;
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
	
	protected abstract EmailConfirmationStatus confirmElements(UserRequestState<?> reqState, T state) 
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
	public EmailConfirmationStatus processConfirmation(String rawState) throws EngineException
	{
		ConfirmationResult confirmResult = doConfirm(rawState);

		txMan.commit();
		
		if (confirmResult.status.isSuccess() && confirmResult.reqState.getStatus().equals(
				RegistrationRequestStatus.pending))
		{
			autoProcess(confirmResult.status, confirmResult.state, confirmResult.reqState, 
					confirmResult.formId);
		}
		
		return confirmResult.status;
	}
	
	private ConfirmationResult doConfirm(String rawState) throws EngineException
	{
		T state = parseState(rawState);
		String requestId = state.getRequestId();

		UserRequestState<?> reqState = null;
		try
		{
			reqState = state.getRequestType() == RequestType.REGISTRATION ? 
					requestDB.get(requestId) : enquiryResponsesDB.get(requestId);
		} catch (Exception e)
		{
			String redirect = new RegistrationRedirectURLBuilder(state.getRedirectUrl(), null,
					requestId, Status.elementConfirmationError).
				setErrorCode("requestDeleted").
				setConfirmationInfo(getConfirmedElementType(state), state.getType(), state.getValue()).
				build();
			return new ConfirmationResult(new EmailConfirmationStatus(false, redirect, 
					"ConfirmationStatus.requestDeleted"));
		}
		
		if (reqState.getStatus().equals(RegistrationRequestStatus.rejected))
		{
			String redirect = new RegistrationRedirectURLBuilder(state.getRedirectUrl(), null,
					requestId, Status.elementConfirmationError).
				setErrorCode("requestRejected").
				setConfirmationInfo(getConfirmedElementType(state), state.getType(), state.getValue()).
				build();
			return new ConfirmationResult(new EmailConfirmationStatus(false, redirect, 
					"ConfirmationStatus.requestRejected"));
		}
		BaseRegistrationInput req = reqState.getRequest();
		EmailConfirmationStatus status = confirmElements(reqState, state);
		
		if (state.getRequestType() == RequestType.REGISTRATION)
			requestDB.update((RegistrationRequestState) reqState);
		else
			enquiryResponsesDB.update((EnquiryResponseState) reqState);
		return new ConfirmationResult(status, state, req.getFormId(), reqState);
	}
	
	private void autoProcess(EmailConfirmationStatus status, T state, UserRequestState<?> reqState, String formId) 
				throws EngineException
	{
		if (state.getRequestType() == RequestType.REGISTRATION)
		{
			RegistrationForm form = formsDB.get(formId);
			String info = "Automatically processing registration request " + state.getRequestId()
			+ " after confirmation [" + state.getType() + "]" + state.getValue() + " by "
			+ state.getFacilityId() + ". Action: {0}";

			sharedRegistrationManagment.autoProcess(form, 
					(RegistrationRequestState) reqState, info);
		} else
		{
			String info = "Automatically processing enquiry response " + state.getRequestId()
			+ " after confirmation [" + state.getType() + "]" + 
			state.getValue() + " by "
			+ state.getFacilityId() + ". Action: {0}";
			EnquiryForm form = enquiresDB.get(formId);
			internalEnquiryManagment.autoProcessEnquiry(form, 
					(EnquiryResponseState) reqState, info);
		}
	}
	
	private class ConfirmationResult
	{
		private EmailConfirmationStatus status;
		private T state;
		private String formId;
		private UserRequestState<?> reqState;
		
		public ConfirmationResult(EmailConfirmationStatus status)
		{
			this.status = status;
		}

		public ConfirmationResult(EmailConfirmationStatus status, T state, String formId, 
				UserRequestState<?> reqState)
		{
			this.status = status;
			this.state = state;
			this.formId = formId;
			this.reqState = reqState;
		}
	}
}
