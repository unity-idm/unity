/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.in.action;

import pl.edu.icm.unity.server.translation.ActionParameterDesc;
import pl.edu.icm.unity.server.translation.ProfileType;
import pl.edu.icm.unity.server.translation.TranslationActionFactory;
import pl.edu.icm.unity.server.translation.in.InputTranslationAction;

/**
 * Boilerplate code for the input profile's {@link TranslationActionFactory} implementations.
 * @author K. Benedyczak
 */
public abstract class AbstractInputTranslationActionFactory implements TranslationActionFactory
{
	private final String name;
	private final ActionParameterDesc[] parameters;
	
	public AbstractInputTranslationActionFactory(String name, ActionParameterDesc... parameters)
	{
		this.name = name;
		this.parameters = parameters;
	}

	@Override
	public ProfileType getSupportedProfileType()
	{
		return ProfileType.INPUT;
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
	public InputTranslationAction getBlindInstance(String... parameters)
	{
		return new BlindStopperInputAction(this, parameters);
	}
}
