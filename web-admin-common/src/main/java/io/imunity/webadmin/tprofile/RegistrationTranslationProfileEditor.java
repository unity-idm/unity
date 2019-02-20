/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webadmin.tprofile;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationActionsRegistry;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.translation.ProfileType;

/**
 * Component to edit or add a registration form translation profile
 * 
 * @author K. Benedyczak
 * 
 */
public class RegistrationTranslationProfileEditor extends TranslationProfileEditor
{
	public RegistrationTranslationProfileEditor(UnityMessageSource msg,
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
