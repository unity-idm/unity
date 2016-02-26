/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.registration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.confirmations.ConfirmationManager;
import pl.edu.icm.unity.confirmations.states.AttribiuteConfirmationState;
import pl.edu.icm.unity.confirmations.states.BaseConfirmationState;
import pl.edu.icm.unity.confirmations.states.IdentityConfirmationState;
import pl.edu.icm.unity.confirmations.states.RegistrationConfirmationState.RequestType;
import pl.edu.icm.unity.confirmations.states.RegistrationReqAttribiuteConfirmationState;
import pl.edu.icm.unity.confirmations.states.RegistrationReqIdentityConfirmationState;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.registries.IdentityTypesRegistry;
import pl.edu.icm.unity.server.registries.RegistrationActionsRegistry;
import pl.edu.icm.unity.server.translation.form.RegistrationTranslationProfile;
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
	private IdentityTypesRegistry identityTypesRegistry;
	private ConfirmationManager confirmationManager;
	private RegistrationActionsRegistry registrationTranslationActionsRegistry;
	
	@Autowired
	public RegistrationConfirmationSupport(IdentityTypesRegistry identityTypesRegistry,
			ConfirmationManager confirmationManager,
			RegistrationActionsRegistry registrationTranslationActionsRegistry)
	{
		this.identityTypesRegistry = identityTypesRegistry;
		this.confirmationManager = confirmationManager;
		this.registrationTranslationActionsRegistry = registrationTranslationActionsRegistry;
	}

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
}
