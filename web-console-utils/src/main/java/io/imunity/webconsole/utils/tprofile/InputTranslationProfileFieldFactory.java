/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.utils.tprofile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.webconsole.tprofile.ActionParameterComponentProviderV8;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationActionsRegistry;

/**
 * Factory for {@link TranslationProfileField}.
 * 
 * @author P.Piernik
 *
 */
@Component
public class InputTranslationProfileFieldFactory extends TranslationProfileFieldFactoryBase
{
	@Autowired
	InputTranslationProfileFieldFactory(MessageSource msg,
			InputTranslationActionsRegistry inputActionsRegistry,
			ActionParameterComponentProviderV8 actionComponentProvider)
	{

		super(msg.getMessage("InputTranslationProfileSection.caption"), msg, ProfileType.INPUT,
				inputActionsRegistry, actionComponentProvider);
	}
}
