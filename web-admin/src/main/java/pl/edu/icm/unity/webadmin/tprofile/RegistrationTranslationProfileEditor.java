/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webadmin.tprofile;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.registries.RegistrationActionsRegistry;
import pl.edu.icm.unity.server.translation.TranslationProfileInstance;
import pl.edu.icm.unity.server.translation.form.RegistrationTranslationProfile;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.webadmin.tprofile.ActionParameterComponentFactory.Provider;

/**
 * Component to edit or add a registration form translation profile
 * 
 * @author K. Benedyczak
 * 
 */
public class RegistrationTranslationProfileEditor extends TranslationProfileEditor
{
	public RegistrationTranslationProfileEditor(UnityMessageSource msg,
			RegistrationActionsRegistry registry, Provider actionComponentProvider,
			RegistrationTranslationProfile toEdit) throws EngineException
	{
		super(msg, registry, ProfileType.REGISTRATION, actionComponentProvider, toEdit);
	}

	@Override
	protected void initUI(TranslationProfileInstance<?, ?> toEdit)
	{
		super.initUI(toEdit);
		name.setVisible(false);
		description.setVisible(false);
	}
}
