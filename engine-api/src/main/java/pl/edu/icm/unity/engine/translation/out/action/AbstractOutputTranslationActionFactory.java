/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.out.action;

import pl.edu.icm.unity.base.translation.ActionParameterDefinition;
import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.base.translation.TranslationActionType;
import pl.edu.icm.unity.engine.api.translation.out.OutputTranslationAction;
import pl.edu.icm.unity.engine.api.translation.out.OutputTranslationActionFactory;

/**
 * Boilerplate code for the output profile's {@link OutputTranslationActionFactory} implementations.
 * @author K. Benedyczak
 */
public abstract class AbstractOutputTranslationActionFactory implements OutputTranslationActionFactory
{
	private TranslationActionType actionType;
	
	public AbstractOutputTranslationActionFactory(String name, ActionParameterDefinition... parameters)
	{
		actionType = new TranslationActionType(ProfileType.OUTPUT, 
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
	public OutputTranslationAction getBlindInstance(String... parameters)
	{
		return new BlindStopperOutputAction(getActionType(), parameters);
	}
}
