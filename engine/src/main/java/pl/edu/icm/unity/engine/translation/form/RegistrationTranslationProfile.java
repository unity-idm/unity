/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.form;

import pl.edu.icm.unity.engine.api.translation.form.RegistrationActionsRegistry;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest;
import pl.edu.icm.unity.engine.attribute.AttributeTypeHelper;
import pl.edu.icm.unity.types.registration.BaseRegistrationInput;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.translation.TranslationProfile;

/**
 * Classic translation profile used for post-processing registration requests.
 * @author K. Benedyczak
 */
public class RegistrationTranslationProfile extends BaseFormTranslationProfile
{
	public RegistrationTranslationProfile(TranslationProfile profile, RegistrationActionsRegistry registry,
			AttributeTypeHelper atHelper, RegistrationForm form)
	{
		super(profile, registry, atHelper, form);
	}
	
	@Override
	protected TranslatedRegistrationRequest initializeTranslationResult(BaseRegistrationInput request)
	{
		TranslatedRegistrationRequest initial = super.initializeTranslationResult(request);
		initial.setCredentialRequirement(((RegistrationForm)form).getDefaultCredentialRequirement());
		return initial;
	}
}
