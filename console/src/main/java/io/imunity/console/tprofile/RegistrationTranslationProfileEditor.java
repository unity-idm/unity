/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.tprofile;

import io.imunity.vaadin.elements.NotificationPresenter;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationActionsRegistry;

/**
 * Component to edit or add a registration form translation profile
 */
public class RegistrationTranslationProfileEditor extends TranslationProfileEditor
{
	public RegistrationTranslationProfileEditor(MessageSource msg,
												RegistrationActionsRegistry registry, ActionParameterComponentProvider actionComponentProvider,
												NotificationPresenter notificationPresenter)
	{
		super(msg, registry, ProfileType.REGISTRATION, actionComponentProvider, notificationPresenter);
	}

	@Override
	protected void initUI()
	{
		super.initUI();
		name.setVisible(false);
		description.setVisible(false);
	}
}
