/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.tprofile;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationActionsRegistry;

/**
 * Component to edit or add a registration form translation profile
 * 
 * @author K. Benedyczak
 * 
 */
public class RegistrationTranslationProfileEditor extends TranslationProfileEditor
{
	public RegistrationTranslationProfileEditor(MessageSource msg,
			RegistrationActionsRegistry registry, ActionParameterComponentProvider actionComponentProvider)
					throws EngineException
	{
		super(msg, registry, ProfileType.REGISTRATION, actionComponentProvider);
	}

	@Override
	protected void initUI()
	{
		super.initUI();
		name.setVisible(false);
		description.setVisible(false);
	}
}
