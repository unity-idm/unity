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
 * Factory for {@link CreateIdentityAction}.
 *   
 * @author K. Benedyczak
 */
@Component
public class CreateIdentityActionFactory implements TranslationActionFactory
{
	public static final String NAME = "createIdentity";
	
	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescriptionKey()
	{
		return "TranslationAction.createIdentity.desc";
	}

	@Override
	public ActionParameterDesc[] getParameters()
	{
		return new ActionParameterDesc[] {
				new ActionParameterDesc(
						"identityType",
						"TranslationAction.createIdentity.paramDesc.idType",
						1, 1, Type.EXPRESSION),
				new ActionParameterDesc(
						"expression",
						"TranslationAction.createIdentity.paramDesc.idValueExpression",
						1, 1, Type.EXPRESSION)};
	}
	
	@Override
	public CreateIdentityAction getInstance(String... parameters) throws EngineException
	{
		return new CreateIdentityAction(parameters, this);
	}

	@Override
	public ProfileType getSupportedProfileType()
	{
		return ProfileType.OUTPUT;
	}
}
