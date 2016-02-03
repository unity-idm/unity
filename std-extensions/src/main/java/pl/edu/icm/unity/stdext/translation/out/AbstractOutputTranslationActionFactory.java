/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.translation.out;

import pl.edu.icm.unity.server.translation.TranslationActionFactory;
import pl.edu.icm.unity.server.translation.TranslationActionInstance;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationActionType;

/**
 * Boilerplate code for the output profile's {@link TranslationActionFactory} implementations.
 * @author K. Benedyczak
 */
public abstract class AbstractOutputTranslationActionFactory implements TranslationActionFactory
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
	public TranslationActionInstance getBlindInstance(String... parameters)
	{
		return new BlindStopperOutputAction(getActionType(), parameters);
	}
}
