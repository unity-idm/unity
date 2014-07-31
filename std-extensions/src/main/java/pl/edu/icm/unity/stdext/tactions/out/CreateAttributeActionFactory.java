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
 * Factory for {@link CreateAttributeAction}.
 *   
 * @author K. Benedyczak
 */
@Component
public class CreateAttributeActionFactory implements TranslationActionFactory
{
	public static final String NAME = "createAttribute";
	
	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescriptionKey()
	{
		return "TranslationAction.createAttribute.desc";
	}

	@Override
	public ActionParameterDesc[] getParameters()
	{
		return new ActionParameterDesc[] {
				new ActionParameterDesc(
						"attributeName",
						"TranslationAction.createAttribute.paramDesc.attributeName",
						1, 1, Type.EXPRESSION),
				new ActionParameterDesc(
						"expression",
						"TranslationAction.createAttribute.paramDesc.expression",
						1, 1, Type.EXPRESSION)
		};
	}
	
	@Override
	public CreateAttributeAction getInstance(String... parameters) throws EngineException
	{
		return new CreateAttributeAction(parameters, this);
	}

	@Override
	public ProfileType getSupportedProfileType()
	{
		return ProfileType.OUTPUT;
	}
}
