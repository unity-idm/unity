/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.confirmation.facilities;

import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationEventPublisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationRedirectURLBuilder.ConfirmedElementType;
import pl.edu.icm.unity.engine.api.confirmation.states.RegistrationEmailConfirmationState;
import pl.edu.icm.unity.engine.api.confirmation.states.RegistrationEmailConfirmationState.RequestType;
import pl.edu.icm.unity.engine.api.finalization.WorkflowFinalizationConfiguration;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.registration.PostFillingHandler;
import pl.edu.icm.unity.engine.forms.enquiry.EnquiryResponseAutoProcessEvent;
import pl.edu.icm.unity.engine.forms.reg.RegistrationRequestAutoProcessEvent;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IdentityExistsException;
import pl.edu.icm.unity.exceptions.RuntimeEngineException;
import pl.edu.icm.unity.store.api.generic.EnquiryFormDB;
import pl.edu.icm.unity.store.api.generic.EnquiryResponseDB;
import pl.edu.icm.unity.store.api.generic.RegistrationFormDB;
import pl.edu.icm.unity.store.api.generic.RegistrationRequestDB;
import pl.edu.icm.unity.store.api.tx.TxManager;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.BaseRegistrationInput;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryResponseState;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.types.registration.RegistrationWrapUpConfig.TriggeringState;
import pl.edu.icm.unity.types.registration.UserRequestState;

/**
 * Common code for processing verifiable elements for elements existing in registration (as opposed to 
 * elements bound to existing entities).
 * 
 * @author K. Benedyczak
 */
public abstract class RegistrationEmailFacility<T extends RegistrationEmailConfirmationState> extends BaseEmailFacility<T>
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER, RegistrationEmailFacility.class);
	
	protected final ObjectMapper mapper = Constants.MAPPER;
	
	protected RegistrationRequestDB requestDB;
	protected EnquiryResponseDB enquiryResponsesDB;
	protected RegistrationFormDB formsDB;
	protected EnquiryFormDB enquiresDB;
	private ApplicationEventPublisher publisher;
	private TxManager txMan;
	private UnityMessageSource msg;


	public RegistrationEmailFacility(RegistrationRequestDB requestDB, EnquiryResponseDB enquiryResponsesDB,
			RegistrationFormDB formsDB, EnquiryFormDB enquiresDB,
			ApplicationEventPublisher publisher,
			TxManager txMan, UnityMessageSource msg)
	{
		this.requestDB = requestDB;
		this.enquiryResponsesDB = enquiryResponsesDB;
		this.formsDB = formsDB;
		this.enquiresDB = enquiresDB;
		this.publisher = publisher;
		this.txMan = txMan;
		this.msg = msg;
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
	
	protected abstract boolean confirmElements(UserRequestState<?> reqState, T state) 
			throws EngineException;
	
	protected abstract ConfirmedElementType getConfirmedElementType(T state);
	
	@Override
	public WorkflowFinalizationConfiguration processConfirmation(String rawState) throws EngineException
	{
		ConfirmationResult confirmResult;
		try
		{
			confirmResult = doConfirm(rawState);
		} catch (FailureWithBehavior e)
		{
			txMan.commit();
			return e.config;
		}
		txMan.commit();
		
		UserRequestState<?> requestState = confirmResult.reqState;
		if (confirmResult.confirmationSuccessful && confirmResult.reqState.getStatus().equals(
				RegistrationRequestStatus.pending))
		{
			try
			{
				autoProcess(confirmResult.confirmationState, confirmResult.reqState, confirmResult.form.getName());
			} catch (RuntimeEngineException e)
			{
				if (confirmResult.type == RequestType.REGISTRATION
						&& e.getCause() instanceof IdentityExistsException)
				{
					return getRegistrationUserExistsFinalizationConfig(
							(RegistrationForm) confirmResult.form, confirmResult.requestId);
				}
				LOG.error(e);
			}
		}
		
		requestState = getRequestState(confirmResult.confirmationState);
		return getConfirmationStatusForForm(
				confirmResult.confirmationSuccessful, 
				confirmResult.form, 
				confirmResult.requestId, 
				requestState.getStatus());
	}
	
	private ConfirmationResult doConfirm(String rawState) throws EngineException
	{
		T confirmationState = parseState(rawState);
		String requestId = confirmationState.getRequestId();

		UserRequestState<?> requestState = getRequestState(confirmationState);
		BaseRegistrationInput request = requestState.getRequest();
		BaseForm form = getRequestForm(confirmationState.getRequestType(), request.getFormId());
		
		if (requestState.getStatus().equals(RegistrationRequestStatus.rejected))
		{
			throw new FailureWithBehavior(getConfirmationStatusForForm(
					false, 
					form, requestId, requestState.getStatus()));
		}
		boolean status = confirmElements(requestState, confirmationState);
		
		if (confirmationState.getRequestType() == RequestType.REGISTRATION)
			requestDB.update((RegistrationRequestState) requestState);
		else
			enquiryResponsesDB.update((EnquiryResponseState) requestState);
		return new ConfirmationResult(status, confirmationState.getRequestType(), 
				form, requestId, requestState, confirmationState);
	}
	
	private UserRequestState<?> getRequestState(T confirmationState) throws EngineException
	{
		String requestId = confirmationState.getRequestId();
		try
		{
			return confirmationState.getRequestType() == RequestType.REGISTRATION ? 
					requestDB.get(requestId) : enquiryResponsesDB.get(requestId);
		} catch (Exception e)
		{
			throw new FailureWithBehavior(WorkflowFinalizationConfiguration.basicError(
					msg.getMessage("ConfirmationStatus.requestDeleted"), null));
		}
	}
	
	private WorkflowFinalizationConfiguration getConfirmationStatusForForm(boolean confirmationSuccessful, 
			BaseForm form, String requestId, RegistrationRequestStatus requestStatus)
	{
		PostFillingHandler postFillingHandler = createPostFillingHandler(form);
		if (requestStatus == RegistrationRequestStatus.pending)
		{
			return postFillingHandler.getFinalRegistrationConfigurationNonSubmit(
					confirmationSuccessful, requestId,
					confirmationSuccessful ? 
					TriggeringState.EMAIL_CONFIRMED : TriggeringState.EMAIL_CONFIRMATION_FAILED);
		} else
		{
			return postFillingHandler
					.getFinalRegistrationConfigurationPostSubmit(requestId, requestStatus);
		}
	}
	
	private WorkflowFinalizationConfiguration getRegistrationUserExistsFinalizationConfig( 
			RegistrationForm regForm, String requestId)
	{
		PostFillingHandler postFillingHandler = createPostFillingHandler(regForm);
		return postFillingHandler.getFinalRegistrationConfigurationNonSubmit(
				false, requestId, TriggeringState.PRESET_USER_EXISTS);
	}
	
	private PostFillingHandler createPostFillingHandler(BaseForm regForm)
	{
		PostFillingHandler postFillingHandler = new PostFillingHandler(
				regForm.getName(), 
				regForm.getWrapUpConfig(), 
				msg,
				regForm.getPageTitle() == null ? null : regForm.getPageTitle().getValue(msg),
						regForm.getLayoutSettings().getLogoURL(), true);
		return postFillingHandler;
	}

	
	private BaseForm getRequestForm(RequestType requestType, String formId)
	{
		return requestType == RequestType.REGISTRATION ? formsDB.get(formId) : enquiresDB.get(formId);
	}
	
	private void autoProcess(T state, UserRequestState<?> reqState, String formId) throws EngineException
	{
		if (state.getRequestType() == RequestType.REGISTRATION)
		{
			RegistrationForm form = formsDB.get(formId);
			String info = "Automatically processing registration request " + state.getRequestId()
				+ " after confirmation [" + state.getType() + "]" + state.getValue() + " by "
				+ state.getFacilityId() + ". Action: {0}";
			publisher.publishEvent(new RegistrationRequestAutoProcessEvent(form, 
					(RegistrationRequestState) reqState, info));
		} else
		{
			String info = "Automatically processing enquiry response " + state.getRequestId()
				+ " after confirmation [" + state.getType() + "]" + 
				state.getValue() + " by "
				+ state.getFacilityId() + ". Action: {0}";
			EnquiryForm form = enquiresDB.get(formId);
			publisher.publishEvent(new EnquiryResponseAutoProcessEvent(form, 
					(EnquiryResponseState) reqState, info));
		}
	}
	
	private class ConfirmationResult
	{
		boolean confirmationSuccessful; 
		RequestType type;
		BaseForm form; 
		String requestId;
		UserRequestState<?> reqState;
		T confirmationState;

		public ConfirmationResult(boolean confirmationSuccessful, RequestType type, BaseForm form,
				String requestId, UserRequestState<?> reqState, T state)
		{
			this.confirmationSuccessful = confirmationSuccessful;
			this.type = type;
			this.form = form;
			this.requestId = requestId;
			this.reqState = reqState;
			this.confirmationState = state;
		}
	}
	
	private static class FailureWithBehavior extends RuntimeException
	{
		WorkflowFinalizationConfiguration config;

		public FailureWithBehavior(WorkflowFinalizationConfiguration config)
		{
			this.config = config;
		}
	}
}
