/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.utils.tprofile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.webadmin.tprofile.ActionParameterComponentProvider;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.translation.out.OutputTranslationActionsRegistry;
import pl.edu.icm.unity.types.translation.ProfileType;

/**
 * Factory for {@link TranslationProfileField}.
 * 
 * @author P.Piernik
 *
 */
@Component
public class OutputTranslationProfileFieldFactory extends TranslationProfileFieldFactoryBase
{
	@Autowired
	OutputTranslationProfileFieldFactory(MessageSource msg,
			OutputTranslationActionsRegistry inputActionsRegistry,
			ActionParameterComponentProvider actionComponentProvider)
	{

		super(msg.getMessage("OutputTranslationProfileSection.caption"), msg, ProfileType.OUTPUT, inputActionsRegistry,
				actionComponentProvider);
	}
}
