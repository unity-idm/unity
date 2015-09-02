/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.tactions.in;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.translation.ActionParameterDesc;
import pl.edu.icm.unity.server.translation.ProfileType;
import pl.edu.icm.unity.server.translation.TranslationActionFactory;
import pl.edu.icm.unity.server.translation.in.InputTranslationAction;

/**
 * Factory of {@link RemoveStaleDataAction}s.
 * @author K. Benedyczak
 */
@Component
public class RemoveStaleDataActionFactory implements TranslationActionFactory
{
	public static final String NAME = "removeStaleData";
	
	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescriptionKey()
	{
		return "TranslationAction.removeStaleData.desc";
	}

	@Override
	public ActionParameterDesc[] getParameters()
	{
		return new ActionParameterDesc[] {};
	}

	@Override
	public InputTranslationAction getInstance(String... parameters) throws EngineException
	{
		return new RemoveStaleDataAction(this, parameters);
	}

	@Override
	public ProfileType getSupportedProfileType()
	{
		return ProfileType.INPUT;
	}
}
