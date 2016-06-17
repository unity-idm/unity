/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.form;

import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.BaseRegistrationInput;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.translation.TranslationProfile;

/**
 * Classic translation profile used for post-processing registration requests.
 * @author K. Benedyczak
 */
public class RegistrationTranslationProfile extends BaseFormTranslationProfile
{
	public RegistrationTranslationProfile(TranslationProfile profile, RegistrationActionsRegistry registry)
	{
		super(profile, registry);
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
