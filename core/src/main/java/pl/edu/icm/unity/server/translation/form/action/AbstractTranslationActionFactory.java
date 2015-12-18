/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.form.action;

import pl.edu.icm.unity.server.translation.ActionParameterDesc;
import pl.edu.icm.unity.server.translation.ProfileType;
import pl.edu.icm.unity.server.translation.RegistrationTranslationActionFactory;
import pl.edu.icm.unity.server.translation.TranslationActionFactory;

/**
 * Boilerplate code for the {@link TranslationActionFactory} implementations.
 * @author K. Benedyczak
 */
public abstract class AbstractTranslationActionFactory implements RegistrationTranslationActionFactory
{
	private final String name;
	private final ActionParameterDesc[] parameters;
	
	
	public AbstractTranslationActionFactory(String name, ActionParameterDesc[] parameters)
	{
		this.name = name;
		this.parameters = parameters;
	}

	@Override
	public ProfileType getSupportedProfileType()
	{
		return ProfileType.REGISTRATION;
	}

	@Override
	public String getDescriptionKey()
	{
		return "RegTranslationAction." + name + ".desc";
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
}
