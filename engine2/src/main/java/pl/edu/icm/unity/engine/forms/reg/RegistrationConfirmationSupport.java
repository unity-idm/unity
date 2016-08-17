/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.forms.reg;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.confirmation.ConfirmationManager;
import pl.edu.icm.unity.engine.api.confirmation.states.AttribiuteConfirmationState;
import pl.edu.icm.unity.engine.api.confirmation.states.BaseConfirmationState;
import pl.edu.icm.unity.engine.api.confirmation.states.IdentityConfirmationState;
import pl.edu.icm.unity.engine.api.confirmation.states.RegistrationConfirmationState.RequestType;
import pl.edu.icm.unity.engine.api.confirmation.states.RegistrationReqAttribiuteConfirmationState;
import pl.edu.icm.unity.engine.api.confirmation.states.RegistrationReqIdentityConfirmationState;
import pl.edu.icm.unity.engine.api.identity.IdentityTypesRegistry;
import pl.edu.icm.unity.engine.attribute.AttributeTypeHelper;
import pl.edu.icm.unity.engine.translation.form.BaseFormTranslationProfile;
import pl.edu.icm.unity.engine.translation.form.EnquiryTranslationProfile;
import pl.edu.icm.unity.engine.translation.form.RegistrationActionsRegistry;
import pl.edu.icm.unity.engine.translation.form.RegistrationTranslationProfile;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
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
	@Autowired
	private AttributeTypeHelper atHelper;
	
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
			Long entityId, BaseForm form, BaseFormTranslationProfile profile) throws EngineException
	{
		for (Attribute attr : requestState.getRequest().getAttributes())
		{
			if (attr == null)
				continue;
			AttributeValueSyntax<?> syntax = atHelper.getUnconfiguredSyntax(attr.getValueSyntax());
			if (syntax.isVerifiable())
			{
				for (String v : attr.getValues())
				{
					VerifiableElement val = (VerifiableElement) syntax.convertFromString(v);
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
			Long entityId, BaseForm form, BaseFormTranslationProfile profile) throws EngineException
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
		return new RegistrationTranslationProfile(translationProfile, registrationTranslationActionsRegistry,
				atHelper);
	}	

	private EnquiryTranslationProfile getEnquiryProfile(EnquiryForm form)
	{
		TranslationProfile translationProfile = form.getTranslationProfile();
		return new EnquiryTranslationProfile(translationProfile, registrationTranslationActionsRegistry,
				atHelper);
	}	
	
	private String getRedirectUrlForAttribute(UserRequestState<?> requestState, BaseForm form,
			Attribute attr, BaseFormTranslationProfile profile)
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
