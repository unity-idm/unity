/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.tactions.out;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.registries.IdentityTypesRegistry;
import pl.edu.icm.unity.server.translation.ActionParameterDesc;
import pl.edu.icm.unity.server.translation.ActionParameterDesc.Type;
import pl.edu.icm.unity.server.translation.ProfileType;
import pl.edu.icm.unity.server.translation.TranslationActionFactory;
import pl.edu.icm.unity.server.translation.out.CreationMode;
import pl.edu.icm.unity.server.translation.out.OutputTranslationAction;

/**
 * Factory for {@link CreateIdentityAction}.
 *   
 * @author K. Benedyczak
 */
@Component
public class CreateIdentityActionFactory implements TranslationActionFactory
{
	public static final String NAME = "createIdentity";
	
	@Autowired
	private IdentityTypesRegistry idTypesReg;
	
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
						1, 1, Type.EXPRESSION),
				new ActionParameterDesc(
						"creationMode",
						"TranslationAction.createIdentity.paramDesc.creationMode",
						1, 1, CreationMode.class)};
	}
	
	@Override
	public OutputTranslationAction getInstance(String... parameters) throws EngineException
	{
		return new CreateIdentityAction(parameters, this, idTypesReg);
	}

	@Override
	public ProfileType getSupportedProfileType()
	{
		return ProfileType.OUTPUT;
	}
}
