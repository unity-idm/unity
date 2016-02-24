/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.form;

import java.util.List;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.confirmations.ConfirmationRedirectURLBuilder;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.registries.RegistrationActionsRegistry;
import pl.edu.icm.unity.server.registries.TypesRegistryBase;
import pl.edu.icm.unity.server.translation.TranslationActionFactory;
import pl.edu.icm.unity.server.translation.form.RegistrationMVELContext.RequestSubmitStatus;
import pl.edu.icm.unity.server.translation.form.action.ConfirmationRedirectActionFactory;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.BaseRegistrationInput;
import pl.edu.icm.unity.types.registration.RegistrationContext;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.translation.TranslationRule;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Classic translation profile used for post-processing registration requests.
 * @author K. Benedyczak
 */
public class RegistrationTranslationProfile extends BaseRegistrationTranslationProfile
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, RegistrationTranslationProfile.class);
	
	public RegistrationTranslationProfile(ObjectNode json, RegistrationActionsRegistry registry)
	{
		super(json, registry);
	}
	
	public RegistrationTranslationProfile(String name, List<? extends TranslationRule> rules, 
			TypesRegistryBase<? extends TranslationActionFactory> registry)
	{
		super(name, rules, registry);
	}
	
	public String getPostConfirmationRedirectURL(RegistrationForm form, RegistrationRequestState request,
			IdentityParam confirmed, String requestId)
	{
		return getPostConfirmationRedirectURL(form, request.getRequest(), request.getRegistrationContext(),
				requestId,
				ConfirmationRedirectURLBuilder.ConfirmedElementType.identity.toString(), 
				confirmed.getTypeId(), confirmed.getValue());
	}

	public String getPostConfirmationRedirectURL(RegistrationForm form, RegistrationRequestState request,
			Attribute<?> confirmed, String requestId)
	{
		return getPostConfirmationRedirectURL(form, request.getRequest(), request.getRegistrationContext(),
				requestId,
				ConfirmationRedirectURLBuilder.ConfirmedElementType.attribute.toString(), 
				confirmed.getName(), confirmed.getValues().get(0).toString());
	}
	
	private String getPostConfirmationRedirectURL(RegistrationForm form, RegistrationRequest request,
			RegistrationContext regContxt, String requestId, 
			String cType, String cName, String cValue)
	{
		RegistrationMVELContext mvelCtx = new RegistrationMVELContext(form, request, 
				RequestSubmitStatus.submitted, 
				regContxt.triggeringMode, regContxt.isOnIdpEndpoint, requestId);
		mvelCtx.addConfirmationContext(cType, cName, cValue);
		TranslatedRegistrationRequest result;
		try
		{
			result = executeFilteredActions(form, 
					request, mvelCtx, ConfirmationRedirectActionFactory.NAME);
		} catch (EngineException e)
		{
			log.error("Couldn't establish redirect URL from profile", e);
			return null;
		}
		
		return "".equals(result.getRedirectURL()) ? null : result.getRedirectURL();
	}
	
	@Override
	protected TranslatedRegistrationRequest initializeTranslationResult(BaseForm form, 
			BaseRegistrationInput request)
	{
		TranslatedRegistrationRequest initial = super.initializeTranslationResult(form, request);
		initial.setCredentialRequirement(((RegistrationForm)form).getDefaultCredentialRequirement());
		return initial;
	}
}
