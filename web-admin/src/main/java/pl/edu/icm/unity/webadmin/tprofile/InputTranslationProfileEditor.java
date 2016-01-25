/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webadmin.tprofile;

import java.util.List;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.registries.InputTranslationActionsRegistry;
import pl.edu.icm.unity.server.translation.in.InputTranslationAction;
import pl.edu.icm.unity.server.translation.in.InputTranslationProfile;
import pl.edu.icm.unity.server.translation.in.InputTranslationRule;
import pl.edu.icm.unity.server.utils.UnityMessageSource;

/**
 * Component to edit or add input translation profile
 * 
 * @author P. Piernik
 * 
 */
public class InputTranslationProfileEditor extends TranslationProfileEditor<InputTranslationAction, InputTranslationRule>
{
	public InputTranslationProfileEditor(UnityMessageSource msg,
			InputTranslationActionsRegistry registry, InputTranslationProfile toEdit, 
			ActionParameterComponentFactory.Provider actionComponentProvider) throws EngineException
	{
		super(msg, registry, toEdit, InputTranslationRule.FACTORY, actionComponentProvider);
	}

	@Override
	public InputTranslationProfile createProfile(String name, List<InputTranslationRule> trules)
	{
		return new InputTranslationProfile(name, trules);
	}
}
