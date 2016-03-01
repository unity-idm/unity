/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.registration;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.confirmations.ConfirmationManager;
import pl.edu.icm.unity.confirmations.states.AttribiuteConfirmationState;
import pl.edu.icm.unity.confirmations.states.BaseConfirmationState;
import pl.edu.icm.unity.confirmations.states.IdentityConfirmationState;
import pl.edu.icm.unity.confirmations.states.RegistrationConfirmationState;
import pl.edu.icm.unity.confirmations.states.RegistrationConfirmationState.RequestType;
import pl.edu.icm.unity.confirmations.states.RegistrationReqAttribiuteConfirmationState;
import pl.edu.icm.unity.confirmations.states.RegistrationReqIdentityConfirmationState;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.api.internal.Token;
import pl.edu.icm.unity.server.api.internal.TokensManagement;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.registries.IdentityTypesRegistry;
import pl.edu.icm.unity.server.registries.RegistrationActionsRegistry;
import pl.edu.icm.unity.server.translation.form.RegistrationTranslationProfile;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.confirmation.VerifiableElement;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryResponseState;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.UserRequestState;
import pl.edu.icm.unity.types.translation.TranslationProfile;

/**
 * Support class implementing sending of confirmation requests for verifiable identities and attributes.
 * Cases when element is already confirmed are detected. Also the implementation handles correctly the
 * case when element is for already created entity or for not created yet.
 * 
 * @author K. Benedyczak
 */
@Component
public class RegistrationConfirmationSupport
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, RegistrationConfirmationSupport.class);
	
	@Autowired
	private IdentityTypesRegistry identityTypesRegistry;
	@Autowired
	private ConfirmationManager confirmationManager;
	@Autowired
	private RegistrationActionsRegistry registrationTranslationActionsRegistry;
	@Autowired
	private TokensManagement tokensMan;
	
	public void sendAttributeConfirmationRequest(RegistrationRequestState requestState,
			Long entityId, RegistrationForm form) throws InternalException, EngineException
	{
		sendAttributeConfirmationRequest(RequestType.REGISTRATION, requestState, entityId, form);
	}

	public void sendAttributeConfirmationRequest(EnquiryResponseState requestState,
			Long entityId, EnquiryForm form) throws InternalException, EngineException
	{
		sendAttributeConfirmationRequest(RequestType.ENQUIRY, requestState, entityId, form);
	}

	public void sendIdentityConfirmationRequest(RegistrationRequestState requestState,
			Long entityId, RegistrationForm form) throws InternalException, EngineException
	{
		sendIdentityConfirmationRequest(RequestType.REGISTRATION, requestState, entityId, form);
	}

	public void sendIdentityConfirmationRequest(EnquiryResponseState requestState,
			Long entityId, EnquiryForm form) throws InternalException, EngineException
	{
		sendIdentityConfirmationRequest(RequestType.ENQUIRY, requestState, entityId, form);
	}
	
	
	private void sendAttributeConfirmationRequest(RequestType type, UserRequestState<?> requestState,
			Long entityId, BaseForm form) throws InternalException, EngineException
	{
		for (Attribute<?> attr : requestState.getRequest().getAttributes())
		{
			if (attr == null)
				continue;
			
			if (attr.getAttributeSyntax().isVerifiable())
			{
				for (Object v : attr.getValues())
				{
					VerifiableElement val = (VerifiableElement) v;
					if (val.isConfirmed())
						continue;
					BaseConfirmationState state;
					if (entityId == null)
					{
						state = new RegistrationReqAttribiuteConfirmationState(
							requestState.getRequestId(), 
							attr.getName(), 
							val.getValue(), 
							requestState.getRequest().getUserLocale(),
							attr.getGroupPath(), 
							getFormRedirectUrlForAttribute(requestState, form, attr),
							type);
					} else
					{
						state = new AttribiuteConfirmationState(
							entityId, 
							attr.getName(), 
							val.getValue(), 
							requestState.getRequest().getUserLocale(), 
							attr.getGroupPath(), 
							getFormRedirectUrlForAttribute(requestState, form, attr));
					}
					confirmationManager.sendConfirmationRequest(state);
				}
			}
		}
	}
	
	private void sendIdentityConfirmationRequest(RequestType requestType, UserRequestState<?> requestState,
			Long entityId, BaseForm form) throws InternalException, EngineException
	{
		for (IdentityParam id : requestState.getRequest().getIdentities())
		{
			if (id == null)
				continue;
			
			if (identityTypesRegistry.getByName(id.getTypeId()).isVerifiable() && !id.isConfirmed())
			{
				BaseConfirmationState state;
				if (entityId == null)
				{
					state = new RegistrationReqIdentityConfirmationState(
							requestState.getRequestId(),
							id.getTypeId(), id.getValue(), 
							requestState.getRequest().getUserLocale(),
							getFormRedirectUrlForIdentity(requestState, form, id),
							requestType);
				} else
				{
					state = new IdentityConfirmationState(entityId, 
							id.getTypeId(), id.getValue(), 
							requestState.getRequest().getUserLocale(),
							getFormRedirectUrlForIdentity(requestState, form, id));
				}
				confirmationManager.sendConfirmationRequest(state);
			}
		}
	}
	
	private String getFormRedirectUrlForIdentity(UserRequestState<?> requestState, BaseForm form,
			IdentityParam identity)
	{
		TranslationProfile translationProfile = form.getTranslationProfile();
		RegistrationTranslationProfile regProfile = new RegistrationTranslationProfile(translationProfile.getName(), 
				translationProfile.getRules(), registrationTranslationActionsRegistry);
		return regProfile.getPostConfirmationRedirectURL(form, requestState, identity, 
				requestState.getRequestId());
	}	
	
	private String getFormRedirectUrlForAttribute(UserRequestState<?> requestState, BaseForm form,
			Attribute<?> attr)
	{
		String current = null;
		if (InvocationContext.getCurrent().getCurrentURLUsed() != null
				&& InvocationContext.getCurrent().getLoginSession() == null)
			current = InvocationContext.getCurrent().getCurrentURLUsed();
		TranslationProfile translationProfile = form.getTranslationProfile();
		RegistrationTranslationProfile regProfile = new RegistrationTranslationProfile(translationProfile.getName(), 
				translationProfile.getRules(), registrationTranslationActionsRegistry);
		String configured = regProfile.getPostConfirmationRedirectURL(form, requestState, attr,
				requestState.getRequestId());
		return configured != null ? configured : current;
	}
	
	/**
	 * Searches for all confirmation tokens bound to the request (all present are not yet confirmed). Those tokens
	 * are rewritten to be bound to a given entity. This method is useful after a request is accepted and 
	 * confirmation should effect in confirming element of existing entity instead of internal request state. 
	 * @param finalRequest
	 * @param entityId
	 * @throws EngineException
	 */
	public void rewriteRequestToken(UserRequestState<?> finalRequest, long entityId) 
			throws EngineException
	{
		List<Token> tks = tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE);
		for (Token tk : tks)
		{
			RegistrationConfirmationState state;
			try
			{
				state = new RegistrationConfirmationState(tk.getContentsString());
			} catch (WrongArgumentException e)
			{
				//OK - not a registration token
				continue;
			}
			if (state.getRequestId().equals(finalRequest.getRequestId()))
			{
				if (state.getFacilityId().equals(
						RegistrationReqAttribiuteConfirmationState.FACILITY_ID))
				{
					rewriteSingleAttributeToken(finalRequest, tk, entityId);
				} else if (state.getFacilityId().equals(
						RegistrationReqIdentityConfirmationState.FACILITY_ID))
				{
					rewriteSingleIdentityToken(finalRequest, tk, entityId);
				}
			}
		}
	}

	private void rewriteSingleAttributeToken(UserRequestState<?> request, Token tk, 
			long entityId) throws EngineException
	{

		RegistrationReqAttribiuteConfirmationState oldState = new RegistrationReqAttribiuteConfirmationState(
				new String(tk.getContents(), StandardCharsets.UTF_8));
		boolean inRequest = false;
		for (Attribute<?> attribute : request.getRequest().getAttributes())
		{
			if (attribute == null || attribute.getAttributeSyntax() == null)
				continue;
			if (inRequest)
				break;
			
			if (attribute.getAttributeSyntax().isVerifiable()
					&& attribute.getName().equals(oldState.getType())
					&& attribute.getValues() != null)

			{
				for (Object o : attribute.getValues())
				{
					VerifiableElement val = (VerifiableElement) o;
					if (val.getValue().equals(oldState.getValue()))
					{
						inRequest = true;
						break;
					}
				}
			}
		}
		tokensMan.removeToken(ConfirmationManager.CONFIRMATION_TOKEN_TYPE, tk.getValue());
		if (inRequest)
		{
			AttribiuteConfirmationState newstate = new AttribiuteConfirmationState(
					entityId, oldState.getType(), oldState.getValue(),
					oldState.getLocale(), oldState.getGroup(),
					oldState.getRedirectUrl());
			log.debug("Update confirmation token " + tk.getValue()
					+ ", changing facility to " + newstate.getFacilityId());
			tokensMan.addToken(ConfirmationManager.CONFIRMATION_TOKEN_TYPE, tk
					.getValue(), newstate.getSerializedConfiguration()
					.getBytes(StandardCharsets.UTF_8), tk.getCreated(), tk
					.getExpires());
		}
	}

	
	private void rewriteSingleIdentityToken(UserRequestState<?> request, Token tk, 
			long entityId) throws EngineException
	{
		RegistrationReqIdentityConfirmationState oldState = new RegistrationReqIdentityConfirmationState(
				new String(tk.getContents(), StandardCharsets.UTF_8));
		boolean inRequest = false;
		for (IdentityParam id : request.getRequest().getIdentities())
		{
			if (id == null)
				continue;
			
			if (id.getTypeId().equals(oldState.getType())
					&& id.getValue().equals(oldState.getValue()))
			{
				inRequest = true;
				break;
			}
		}

		tokensMan.removeToken(ConfirmationManager.CONFIRMATION_TOKEN_TYPE, tk.getValue());
		if (inRequest)
		{
			IdentityConfirmationState newstate = new IdentityConfirmationState(
					entityId, oldState.getType(), oldState.getValue(),
					oldState.getLocale(), oldState.getRedirectUrl());
			log.debug("Update confirmation token " + tk.getValue()
					+ ", changing facility to " + newstate.getFacilityId());
			tokensMan.addToken(ConfirmationManager.CONFIRMATION_TOKEN_TYPE, tk
					.getValue(), newstate.getSerializedConfiguration()
					.getBytes(StandardCharsets.UTF_8), tk.getCreated(), tk
					.getExpires());
		}

	}

}
