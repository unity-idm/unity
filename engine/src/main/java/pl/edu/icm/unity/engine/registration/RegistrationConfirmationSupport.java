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
import pl.edu.icm.unity.server.translation.form.BaseFormTranslationProfile;
import pl.edu.icm.unity.server.translation.form.EnquiryTranslationProfile;
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
 * Cases when element is already confirmed are detected.
 * 
 * @author K. Benedyczak
 */
@Component
public class RegistrationConfirmationSupport
{
	@Autowired
	private IdentityTypesRegistry identityTypesRegistry;
	@Autowired
	private ConfirmationManager confirmationManager;
	@Autowired
	private RegistrationActionsRegistry registrationTranslationActionsRegistry;
	
	public void sendAttributeConfirmationRequest(RegistrationRequestState requestState,
			Long entityId, RegistrationForm form) throws InternalException, EngineException
	{
		sendAttributeConfirmationRequest(RequestType.REGISTRATION, requestState, entityId, form,
				getRegistrationProfile(form));
	}

	public void sendAttributeConfirmationRequest(EnquiryResponseState requestState,
			EnquiryForm form, Long entityId) throws InternalException, EngineException
	{
		sendAttributeConfirmationRequest(RequestType.ENQUIRY, requestState, entityId, form,
				getEnquiryProfile(form));
	}

	public void sendIdentityConfirmationRequest(RegistrationRequestState requestState,
			Long entityId, RegistrationForm form) throws InternalException, EngineException
	{
		sendIdentityConfirmationRequest(RequestType.REGISTRATION, requestState, entityId, form,
				getRegistrationProfile(form));
	}

	public void sendIdentityConfirmationRequest(EnquiryResponseState requestState,
			EnquiryForm form, Long entityId) throws InternalException, EngineException
	{
		sendIdentityConfirmationRequest(RequestType.ENQUIRY, requestState, entityId, form,
				getEnquiryProfile(form));
	}
	
	
	private void sendAttributeConfirmationRequest(RequestType type, UserRequestState<?> requestState,
			Long entityId, BaseForm form, BaseFormTranslationProfile profile) throws InternalException, EngineException
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
							getRedirectUrlForAttribute(requestState, form, attr, profile),
							type);
					} else
					{
						state = new AttribiuteConfirmationState(
							entityId, 
							attr.getName(), 
							val.getValue(), 
							requestState.getRequest().getUserLocale(), 
							attr.getGroupPath(), 
							getRedirectUrlForAttribute(requestState, form, attr, profile));
					}
					confirmationManager.sendConfirmationRequest(state);
				}
			}
		}
	}
	
	private void sendIdentityConfirmationRequest(RequestType requestType, UserRequestState<?> requestState,
			Long entityId, BaseForm form, BaseFormTranslationProfile profile) throws InternalException, EngineException
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
							getRedirectUrlForIdentity(requestState, form, id, profile),
							requestType);
				} else
				{
					state = new IdentityConfirmationState(entityId, 
							id.getTypeId(), id.getValue(), 
							requestState.getRequest().getUserLocale(),
							getRedirectUrlForIdentity(requestState, form, id, profile));
				}
				confirmationManager.sendConfirmationRequest(state);
			}
		}
	}

	private String getRedirectUrlForIdentity(UserRequestState<?> requestState, BaseForm form,
			IdentityParam identity, BaseFormTranslationProfile profile)
	{
		return profile.getPostConfirmationRedirectURL(form, requestState, identity, 
				requestState.getRequestId());
	}
	
	private RegistrationTranslationProfile getRegistrationProfile(RegistrationForm form)
	{
		TranslationProfile translationProfile = form.getTranslationProfile();
		return new RegistrationTranslationProfile(translationProfile.getName(), 
				translationProfile.getRules(), registrationTranslationActionsRegistry);
	}	

	private EnquiryTranslationProfile getEnquiryProfile(EnquiryForm form)
	{
		TranslationProfile translationProfile = form.getTranslationProfile();
		return new EnquiryTranslationProfile(translationProfile.getName(), 
				translationProfile.getRules(), registrationTranslationActionsRegistry);
	}	
	
	private String getRedirectUrlForAttribute(UserRequestState<?> requestState, BaseForm form,
			Attribute<?> attr, BaseFormTranslationProfile profile)
	{
		String current = null;
		if (InvocationContext.getCurrent().getCurrentURLUsed() != null
				&& InvocationContext.getCurrent().getLoginSession() == null)
			current = InvocationContext.getCurrent().getCurrentURLUsed();
		String configured = profile.getPostConfirmationRedirectURL(form, requestState, attr,
				requestState.getRequestId());
		return configured != null ? configured : current;
	}
}
