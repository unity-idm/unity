/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.tprofile;

import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.api.HtmlTooltipFactory;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationActionsRegistry;

import java.util.Set;

/**
 * Component to edit or add a registration form translation profile
 */
public class RegistrationTranslationProfileEditor extends TranslationProfileEditor
{
	public RegistrationTranslationProfileEditor(MessageSource msg, RegistrationActionsRegistry registry,
			ActionParameterComponentProvider actionComponentProvider, NotificationPresenter notificationPresenter,
			HtmlTooltipFactory htmlTooltipFactory)
	{
		super(msg, registry, ProfileType.REGISTRATION, actionComponentProvider, notificationPresenter,
				htmlTooltipFactory, Set.of());
	}

	@Override
	protected void initUI()
	{
		super.initUI();
		name.getParent().ifPresent(parent -> parent.setVisible(false));
		description.getParent().ifPresent(parent -> parent.setVisible(false));
	}
}
