/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.in.action;

import pl.edu.icm.unity.base.translation.ActionParameterDefinition;
import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.base.translation.TranslationActionType;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationAction;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationActionFactory;

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
