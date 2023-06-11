/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.form;

import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.base.registration.EnquiryResponse;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationActionsRegistry;
import pl.edu.icm.unity.engine.attribute.AttributeTypeHelper;

/**
 * Classic translation profile used for post-processing of {@link EnquiryResponse}s.
 * Delegates to super class.
 * @author K. Benedyczak
 */
public class EnquiryTranslationProfile extends BaseFormTranslationProfile
{
	public EnquiryTranslationProfile(TranslationProfile profile, RegistrationActionsRegistry registry,
			AttributeTypeHelper atHelper, EnquiryForm form)
	{
		super(profile, registry, atHelper, form);
	}
}
