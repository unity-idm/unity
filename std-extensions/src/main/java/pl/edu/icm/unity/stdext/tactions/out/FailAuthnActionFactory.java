/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.tactions.out;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.translation.ActionParameterDesc;
import pl.edu.icm.unity.server.translation.ActionParameterDesc.Type;
import pl.edu.icm.unity.server.translation.ProfileType;
import pl.edu.icm.unity.server.translation.TranslationActionFactory;

/**
 * Factory for {@link FailAuthnAction}.
 *   
 * @author K. Benedyczak
 */
@Component
public class FailAuthnActionFactory implements TranslationActionFactory
{
	public static final String NAME = "failAuthentication";
	
	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescriptionKey()
	{
		return "TranslationAction.failAuthentication.desc";
	}

	@Override
	public ActionParameterDesc[] getParameters()
	{
		return new ActionParameterDesc[] {
				new ActionParameterDesc(
						"message",
						"TranslationAction.failAuthentication.paramDesc.message",
						1, 1, Type.LARGE_TEXT)
		};
	}
	
	@Override
	public FailAuthnAction getInstance(String... parameters) throws EngineException
	{
		return new FailAuthnAction(parameters, this);
	}

	@Override
	public ProfileType getSupportedProfileType()
	{
		return ProfileType.OUTPUT;
	}
}
