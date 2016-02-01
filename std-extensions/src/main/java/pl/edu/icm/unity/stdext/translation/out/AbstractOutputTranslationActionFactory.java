/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.translation.out;

import pl.edu.icm.unity.server.translation.ActionParameterDesc;
import pl.edu.icm.unity.server.translation.ProfileType;
import pl.edu.icm.unity.server.translation.TranslationAction;
import pl.edu.icm.unity.server.translation.TranslationActionFactory;

/**
 * Boilerplate code for the output profile's {@link TranslationActionFactory} implementations.
 * @author K. Benedyczak
 */
public abstract class AbstractOutputTranslationActionFactory implements TranslationActionFactory
{
	private final String name;
	private final ActionParameterDesc[] parameters;
	
	public AbstractOutputTranslationActionFactory(String name, ActionParameterDesc... parameters)
	{
		this.name = name;
		this.parameters = parameters;
	}
		
	@Override
	public ProfileType getSupportedProfileType()
	{
		return ProfileType.OUTPUT;
	}

	@Override
	public String getDescriptionKey()
	{
		return "TranslationAction." + name + ".desc";
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public ActionParameterDesc[] getParameters()
	{
		return parameters;
	}
	
	@Override
	public TranslationAction getBlindInstance(String... parameters)
	{
		return new BlindStopperOutputAction(this, parameters);
	}
}
