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
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationActionsRegistry;


@Component
public class InputTranslationProfileFieldFactory extends TranslationProfileFieldFactoryBase
{
	InputTranslationProfileFieldFactory(MessageSource msg,
			InputTranslationActionsRegistry inputActionsRegistry,
			ActionParameterComponentProvider actionComponentProvider, NotificationPresenter notificationPresenter,
			HtmlTooltipFactory htmlTooltipFactory)
	{

		super(msg.getMessage("InputTranslationProfileSection.caption"), msg, ProfileType.INPUT,
				inputActionsRegistry, actionComponentProvider, notificationPresenter, htmlTooltipFactory);
	}
}
