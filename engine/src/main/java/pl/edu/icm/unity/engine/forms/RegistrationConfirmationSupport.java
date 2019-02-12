/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.forms;

import java.util.List;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationManager;
import pl.edu.icm.unity.engine.api.confirmation.states.BaseEmailConfirmationState;
import pl.edu.icm.unity.engine.api.confirmation.states.EmailAttribiuteConfirmationState;
import pl.edu.icm.unity.engine.api.confirmation.states.EmailIdentityConfirmationState;
import pl.edu.icm.unity.engine.api.confirmation.states.RegistrationEmailConfirmationState.RequestType;
import pl.edu.icm.unity.engine.api.confirmation.states.RegistrationReqEmailAttribiuteConfirmationState;
import pl.edu.icm.unity.engine.api.confirmation.states.RegistrationReqEmailIdentityConfirmationState;
import pl.edu.icm.unity.engine.api.identity.IdentityTypesRegistry;
import pl.edu.icm.unity.engine.api.registration.FormAutomationSupport;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationActionsRegistry;
import pl.edu.icm.unity.engine.attribute.AttributeTypeHelper;
import pl.edu.icm.unity.engine.group.GroupHelper;
import pl.edu.icm.unity.engine.translation.form.EnquiryTranslationProfile;
import pl.edu.icm.unity.engine.translation.form.FormAutomationSupportExt;
import pl.edu.icm.unity.engine.translation.form.RegistrationTranslationProfile;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.confirmation.VerifiableElement;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.ConfirmationMode;
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
	public enum Phase 
	{
		ON_SUBMIT(ConfirmationMode.ON_SUBMIT), 
		ON_ACCEPT(ConfirmationMode.ON_ACCEPT);
		
		private ConfirmationMode supportedMode;

		private Phase(ConfirmationMode supportedMode)
		{
			this.supportedMode = supportedMode;
		}
	}
	
	@Autowired
	private IdentityTypesRegistry identityTypesRegistry;
	@Autowired
	private EmailConfirmationManager confirmationManager;
	@Autowired
	private RegistrationActionsRegistry registrationTranslationActionsRegistry;
	@Autowired
	private AttributeTypeHelper atHelper;
	@Autowired
	private ObjectFactory<FormAutomationSupportExt> formAutomationSupportFactory;
	@Autowired
	private GroupHelper groupHelper;
	
	public void sendAttributeConfirmationRequest(RegistrationRequestState requestState,
			Long entityId, RegistrationForm form, Phase phase) throws InternalException, EngineException
	{
		sendAttributeConfirmationRequest(RequestType.REGISTRATION, requestState, entityId, form, phase);
	}

	public void sendAttributeConfirmationRequest(EnquiryResponseState requestState,
			EnquiryForm form, Long entityId, Phase phase) throws InternalException, EngineException
	{
		sendAttributeConfirmationRequest(RequestType.ENQUIRY, requestState, entityId, form, phase);
	}

	public void sendIdentityConfirmationRequest(RegistrationRequestState requestState,
			Long entityId, RegistrationForm form, Phase phase) throws InternalException, EngineException
	{
		sendIdentityConfirmationRequest(RequestType.REGISTRATION, requestState, entityId, form, phase);
	}

	public void sendIdentityConfirmationRequest(EnquiryResponseState requestState,
			EnquiryForm form, Long entityId, Phase phase) throws InternalException, EngineException
	{
		sendIdentityConfirmationRequest(RequestType.ENQUIRY, requestState, entityId, form, phase);
	}
	
	
	private void sendAttributeConfirmationRequest(RequestType type, UserRequestState<?> requestState,
			Long entityId, BaseForm form, Phase phase) throws EngineException
	{
		List<Attribute> attributes = requestState.getRequest().getAttributes();
		for (int i=0; i<attributes.size(); i++)
		{
			Attribute attr = attributes.get(i);
			if (attr == null)
				continue;
			if (phase.supportedMode != form.getAttributeParams().get(i).getConfirmationMode())
				continue;
			if (entityId != null)
			{
				if (groupHelper.isMember(entityId, attr.getGroupPath()))
					continue;
			}
			
			AttributeValueSyntax<?> syntax = atHelper.getUnconfiguredSyntax(attr.getValueSyntax());
			if (syntax.isEmailVerifiable())
			{
				for (String v : attr.getValues())
				{
					VerifiableElement val = (VerifiableElement) syntax.convertFromString(v);
					if (val.isConfirmed())
						continue;
					BaseEmailConfirmationState state;
					if (entityId == null)
					{
						state = new RegistrationReqEmailAttribiuteConfirmationState(
							requestState.getRequestId(), 
							attr.getName(), 
							val.getValue(), 
							requestState.getRequest().getUserLocale(),
							attr.getGroupPath(),
							type);
					} else
					{
						state = new EmailAttribiuteConfirmationState(
							entityId, 
							attr.getName(), 
							val.getValue(), 
							requestState.getRequest().getUserLocale(), 
							attr.getGroupPath());
					}
					confirmationManager.sendConfirmationRequest(state);
				}
			}
		}
	}
	
	private void sendIdentityConfirmationRequest(RequestType requestType, UserRequestState<?> requestState,
			Long entityId, BaseForm form, Phase phase) throws EngineException
	{
		List<IdentityParam> identities = requestState.getRequest().getIdentities();
		for (int i=0; i<identities.size(); i++)
		{
			IdentityParam id = identities.get(i);
			if (id == null)
				continue;
			if (phase.supportedMode != form.getIdentityParams().get(i).getConfirmationMode())
				continue;
			if (identityTypesRegistry.getByName(id.getTypeId()).isEmailVerifiable() && !id.isConfirmed())
			{
				BaseEmailConfirmationState state;
				if (entityId == null)
				{
					state = new RegistrationReqEmailIdentityConfirmationState(
							requestState.getRequestId(),
							id.getTypeId(), id.getValue(), 
							requestState.getRequest().getUserLocale(),
							requestType);
				} else
				{
					state = new EmailIdentityConfirmationState(entityId, 
							id.getTypeId(), id.getValue(), 
							requestState.getRequest().getUserLocale());
				}
				confirmationManager.sendConfirmationRequest(state);
			}
		}
	}

	private RegistrationTranslationProfile getRegistrationProfile(RegistrationForm form)
	{
		TranslationProfile translationProfile = form.getTranslationProfile();
		return new RegistrationTranslationProfile(translationProfile, registrationTranslationActionsRegistry,
				atHelper, form);
	}	

	public FormAutomationSupport getRegistrationFormAutomationSupport(RegistrationForm form)
	{
		FormAutomationSupportExt automationSupport = formAutomationSupportFactory.getObject();
		automationSupport.init(getRegistrationProfile(form));
		return automationSupport;
	}
	
	public FormAutomationSupport getEnquiryFormAutomationSupport(EnquiryForm form)
	{
		FormAutomationSupportExt automationSupport = formAutomationSupportFactory.getObject();
		automationSupport.init(getEnquiryProfile(form));
		return automationSupport;
	}
	
	private EnquiryTranslationProfile getEnquiryProfile(EnquiryForm form)
	{
		TranslationProfile translationProfile = form.getTranslationProfile();
		return new EnquiryTranslationProfile(translationProfile, registrationTranslationActionsRegistry,
				atHelper, form);
	}	
}
