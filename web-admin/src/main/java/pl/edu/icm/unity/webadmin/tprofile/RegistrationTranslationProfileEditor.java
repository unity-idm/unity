/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webadmin.tprofile;

import java.util.List;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.registries.RegistrationActionsRegistry;
import pl.edu.icm.unity.server.translation.TranslationProfile;
import pl.edu.icm.unity.server.translation.form.RegistrationTranslationAction;
import pl.edu.icm.unity.server.translation.form.RegistrationTranslationProfile;
import pl.edu.icm.unity.server.translation.form.RegistrationTranslationRule;
import pl.edu.icm.unity.server.utils.UnityMessageSource;

/**
 * Component to edit or add a registration form translation profile
 * 
 * @author K. Benedyczak
 * 
 */
public class RegistrationTranslationProfileEditor extends TranslationProfileEditor<RegistrationTranslationAction, 
			RegistrationTranslationRule>
{
	public RegistrationTranslationProfileEditor(UnityMessageSource msg,
			RegistrationActionsRegistry registry, RegistrationTranslationProfile toEdit, 
			ActionParameterComponentFactory.Provider actionComponentProvider) throws EngineException
	{
		super(msg, registry, toEdit, RegistrationTranslationRule.FACTORY, actionComponentProvider);
	}

	@Override
	public RegistrationTranslationProfile createProfile(String name, List<RegistrationTranslationRule> trules)
	{
		return new RegistrationTranslationProfile(name, trules);
	}
	
	@Override
	protected void initUI(TranslationProfile<RegistrationTranslationAction> toEdit)
	{
		super.initUI(toEdit);
		name.setVisible(false);
		description.setVisible(false);
	}
}
