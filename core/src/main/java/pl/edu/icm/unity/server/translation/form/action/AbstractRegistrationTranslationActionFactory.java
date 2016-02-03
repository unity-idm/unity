/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.form.action;

import pl.edu.icm.unity.server.translation.TranslationActionInstance;
import pl.edu.icm.unity.server.translation.form.RegistrationTranslationActionFactory;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationActionType;

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
	public TranslationActionInstance getBlindInstance(String... parameters)
	{
		return new BlindStopperRegistrationAction(getActionType(), parameters);
	}
}
