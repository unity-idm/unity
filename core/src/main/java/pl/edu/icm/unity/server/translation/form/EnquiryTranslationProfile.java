/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.form;

import java.util.List;

import pl.edu.icm.unity.server.registries.RegistrationActionsRegistry;
import pl.edu.icm.unity.server.registries.TypesRegistryBase;
import pl.edu.icm.unity.server.translation.TranslationActionFactory;
import pl.edu.icm.unity.types.registration.EnquiryResponse;
import pl.edu.icm.unity.types.translation.TranslationRule;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Classic translation profile used for post-processing of {@link EnquiryResponse}s.
 * Delegates to super class.
 * @author K. Benedyczak
 */
public class EnquiryTranslationProfile extends BaseRegistrationTranslationProfile
{
	public EnquiryTranslationProfile(ObjectNode json, RegistrationActionsRegistry registry)
	{
		super(json, registry);
	}
	
	public EnquiryTranslationProfile(String name, List<? extends TranslationRule> rules, 
			TypesRegistryBase<? extends TranslationActionFactory> registry)
	{
		super(name, rules, registry);
	}
}
