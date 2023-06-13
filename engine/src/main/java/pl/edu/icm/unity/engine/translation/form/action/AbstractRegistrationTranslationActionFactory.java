/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.form.action;

import pl.edu.icm.unity.base.translation.ActionParameterDefinition;
import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.base.translation.TranslationActionType;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationTranslationAction;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationTranslationActionFactory;

/**
 * Boilerplate code for the {@link RegistrationTranslationActionFactory} implementations.
 * @author K. Benedyczak
 */
public abstract class AbstractRegistrationTranslationActionFactory implements RegistrationTranslationActionFactory
{
	private TranslationActionType actionType;
	
	public AbstractRegistrationTranslationActionFactory(String name, ActionParameterDefinition[] parameters)
	{
		       actionType = new TranslationActionType(ProfileType.REGISTRATION,
			                "RegTranslationAction." + name + ".desc",
			                name,
			                parameters);
			  
	}

	@Override
	public TranslationActionType getActionType()
	{
		return actionType;
	}
	
	@Override
	public RegistrationTranslationAction getBlindInstance(String... parameters)
	{
		return new BlindStopperRegistrationAction(getActionType(), parameters);
	}
}
