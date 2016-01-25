/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formman;

import pl.edu.icm.unity.server.registries.RegistrationActionsRegistry;
import pl.edu.icm.unity.server.translation.form.RegistrationTranslationAction;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webadmin.tprofile.TranslationProfileViewer;

/**
 * Simple extension of the {@link TranslationProfileViewer}, hiding name and description 
 * of the profile, which is not relevant in the profile used in a registration form.
 * @author K. Benedyczak
 */
public class RegistrationTranslationProfileViewer extends TranslationProfileViewer<RegistrationTranslationAction>
{

	public RegistrationTranslationProfileViewer(UnityMessageSource msg,
			RegistrationActionsRegistry registry)
	{
		super(msg, registry);
	}

	protected void initUI()
	{
		super.initUI();
		name.setVisible(false);
		description.setVisible(false);
	}
}
