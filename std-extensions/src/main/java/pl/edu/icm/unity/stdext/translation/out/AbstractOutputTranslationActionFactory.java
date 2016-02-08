/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.translation.out;

import pl.edu.icm.unity.server.translation.out.OutputTranslationAction;
import pl.edu.icm.unity.server.translation.out.OutputTranslationActionFactory;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationActionType;

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
