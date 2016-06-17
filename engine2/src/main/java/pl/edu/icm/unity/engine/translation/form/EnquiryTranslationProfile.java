/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.form;

import pl.edu.icm.unity.types.registration.EnquiryResponse;
import pl.edu.icm.unity.types.translation.TranslationProfile;

/**
 * Classic translation profile used for post-processing of {@link EnquiryResponse}s.
 * Delegates to super class.
 * @author K. Benedyczak
 */
public class EnquiryTranslationProfile extends BaseFormTranslationProfile
{
	public EnquiryTranslationProfile(TranslationProfile profile, RegistrationActionsRegistry registry)
	{
		super(profile, registry);
	}
}
