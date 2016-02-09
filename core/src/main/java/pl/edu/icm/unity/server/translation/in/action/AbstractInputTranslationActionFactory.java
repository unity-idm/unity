/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.in.action;

import pl.edu.icm.unity.server.translation.in.InputTranslationAction;
import pl.edu.icm.unity.server.translation.in.InputTranslationActionFactory;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationActionType;

/**
 * Boilerplate code for the input profile's {@link InputTranslationActionFactory} implementations.
 * @author K. Benedyczak
 */
public abstract class AbstractInputTranslationActionFactory implements InputTranslationActionFactory
{
	private TranslationActionType actionType;

	public AbstractInputTranslationActionFactory(String name, ActionParameterDefinition... parameters)
	{
		actionType = new TranslationActionType(ProfileType.INPUT,
				"TranslationAction." + name + ".desc",
				name,
				parameters);

	}

	@Override
	public TranslationActionType getActionType()
	{
		return actionType;
	}

	@Override
	public InputTranslationAction getBlindInstance(String... parameters)
	{
		return new BlindStopperInputAction(getActionType(), parameters);
	}
}
