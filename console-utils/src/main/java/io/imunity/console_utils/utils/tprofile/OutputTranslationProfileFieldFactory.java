/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console_utils.utils.tprofile;

import io.imunity.console_utils.tprofile.ActionParameterComponentProvider;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.api.HtmlTooltipFactory;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.engine.api.translation.out.OutputTranslationActionsRegistry;


@Component
public class OutputTranslationProfileFieldFactory extends TranslationProfileFieldFactoryBase
{
	OutputTranslationProfileFieldFactory(MessageSource msg,
			OutputTranslationActionsRegistry inputActionsRegistry,
			ActionParameterComponentProvider actionComponentProvider, NotificationPresenter notificationPresenter,
			HtmlTooltipFactory htmlTooltipFactory)
	{

		super(msg.getMessage("OutputTranslationProfileSection.caption"), msg, ProfileType.OUTPUT, inputActionsRegistry,
				actionComponentProvider, notificationPresenter, htmlTooltipFactory);
	}
}
