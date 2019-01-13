/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.idp;

import java.util.ArrayList;
import java.util.List;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.translation.out.OutputTranslationActionsRegistry;
import pl.edu.icm.unity.engine.api.translation.out.TranslationInput;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.engine.attribute.AttributeValueConverter;
import pl.edu.icm.unity.engine.translation.out.OutputTranslationEngine;
import pl.edu.icm.unity.engine.translation.out.OutputTranslationProfile;
import pl.edu.icm.unity.engine.translation.out.OutputTranslationProfileRepository;
import pl.edu.icm.unity.engine.translation.out.action.CreateAttributeActionFactory;
import pl.edu.icm.unity.engine.translation.out.action.FilterAttributeActionFactory;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationAction;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.types.translation.TranslationRule;

/**
 * Instantiates and executes output profile on provided input. Supports creation of the default
 * profile if needed.
 *    
 * @author K. Benedyczak
 */
class OutputProfileExecutor
{
	private final OutputTranslationEngine translationEngine;
	private final OutputTranslationProfileRepository outputProfileRepo;
	private final OutputTranslationActionsRegistry actionsRegistry;
	private final UnityMessageSource msg;
	private final AttributeValueConverter attrValueConverter; 
	private final OutputTranslationProfile defaultProfile;

	
	OutputProfileExecutor(OutputTranslationProfileRepository outputProfileRepo,
			OutputTranslationEngine translationEngine,
			OutputTranslationActionsRegistry actionsRegistry,
			AttributeValueConverter attrValueConverter,
			UnityMessageSource msg)
	{
		this.translationEngine = translationEngine;
		this.outputProfileRepo = outputProfileRepo;
		this.actionsRegistry = actionsRegistry;
		this.attrValueConverter = attrValueConverter;
		this.msg = msg;

		this.defaultProfile = createDefaultOutputProfile();
	}

	TranslationResult execute(String profile, TranslationInput input) throws EngineException
	{
		OutputTranslationProfile profileInstance;
		if (profile != null)
		{
			TranslationProfile translationProfile = outputProfileRepo.listAllProfiles().get(profile);
			if (translationProfile == null)
				throw new ConfigurationException("The translation profile '" + profile + 
					"' configured for the authenticator does not exist");
			profileInstance = new OutputTranslationProfile(translationProfile, outputProfileRepo, 
					actionsRegistry, attrValueConverter);
		} else
		{
			profileInstance = defaultProfile;
		}
		TranslationResult result = profileInstance.translate(input);
		translationEngine.process(input, result);
		return result;
	}

	private OutputTranslationProfile createDefaultOutputProfile()
	{
		List<TranslationRule> rules = new ArrayList<>();
		TranslationAction action1 = new TranslationAction(CreateAttributeActionFactory.NAME, 
				new String[] {"memberOf", "groups", "false", 
						msg.getMessage("DefaultOutputTranslationProfile.attr.memberOf"), 
						msg.getMessage("DefaultOutputTranslationProfile.attr.memberOfDesc")});
		rules.add(new TranslationRule("true", action1));
		TranslationAction action2 = new TranslationAction(FilterAttributeActionFactory.NAME,
				"sys:.*");
		rules.add(new TranslationRule("true", action2));
		TranslationProfile profile = new TranslationProfile("DEFAULT OUTPUT PROFILE", "", ProfileType.OUTPUT,
				rules);
		return new OutputTranslationProfile(profile, outputProfileRepo, actionsRegistry, attrValueConverter);
	}
}
