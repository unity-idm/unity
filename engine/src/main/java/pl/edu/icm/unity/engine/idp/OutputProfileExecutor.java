/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.idp;

import java.util.ArrayList;
import java.util.List;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.base.translation.TranslationAction;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.base.translation.TranslationRule;
import pl.edu.icm.unity.engine.api.AttributeValueConverter;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.translation.out.OutputTranslationActionsRegistry;
import pl.edu.icm.unity.engine.api.translation.out.TranslationInput;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.engine.translation.out.OutputTranslationEngine;
import pl.edu.icm.unity.engine.translation.out.OutputTranslationProfile;
import pl.edu.icm.unity.engine.translation.out.OutputTranslationProfileRepository;
import pl.edu.icm.unity.engine.translation.out.action.CreateAttributeActionFactory;
import pl.edu.icm.unity.engine.translation.out.action.FilterAttributeActionFactory;

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
	private final MessageSource msg;
	private final AttributeValueConverter attrValueConverter; 
	private final OutputTranslationProfile defaultProfile;
	private final GroupsManagement groupsManagement;

	
	OutputProfileExecutor(OutputTranslationProfileRepository outputProfileRepo,
			OutputTranslationEngine translationEngine,
			OutputTranslationActionsRegistry actionsRegistry,
			AttributeValueConverter attrValueConverter,
			MessageSource msg, GroupsManagement groupsManagement)
	{
		this.translationEngine = translationEngine;
		this.outputProfileRepo = outputProfileRepo;
		this.actionsRegistry = actionsRegistry;
		this.attrValueConverter = attrValueConverter;
		this.msg = msg;
		this.groupsManagement = groupsManagement;
		this.defaultProfile = createDefaultOutputProfile();
	}

	TranslationResult execute(TranslationProfile profile, TranslationInput input) throws EngineException
	{
		OutputTranslationProfile profileInstance;
		if (profile != null)
		{
			profileInstance = new OutputTranslationProfile(profile, outputProfileRepo, 
					actionsRegistry, attrValueConverter, groupsManagement);
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
		return new OutputTranslationProfile(profile, outputProfileRepo, actionsRegistry, attrValueConverter, groupsManagement);
	}
}
