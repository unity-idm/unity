/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.form;

import java.util.List;

import pl.edu.icm.unity.server.registries.RegistrationActionsRegistry;
import pl.edu.icm.unity.server.registries.TypesRegistryBase;
import pl.edu.icm.unity.server.translation.TranslationActionFactory;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.BaseRegistrationInput;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.translation.TranslationRule;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Classic translation profile used for post-processing registration requests.
 * @author K. Benedyczak
 */
public class RegistrationTranslationProfile extends BaseRegistrationTranslationProfile
{
	public RegistrationTranslationProfile(ObjectNode json, RegistrationActionsRegistry registry)
	{
		super(json, registry);
	}
	
	public RegistrationTranslationProfile(String name, List<? extends TranslationRule> rules, 
			TypesRegistryBase<? extends TranslationActionFactory> registry)
	{
		super(name, rules, registry);
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
