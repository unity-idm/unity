/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.tactions;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.authn.remote.translation.ActionParameterDesc;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationAction;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationActionFactory;

/**
 * Factory for {@link MapAttributeToIdentityAction}.
 *   
 * @author K. Benedyczak
 */
@Component
public class MapIdentityByTypeActionFactory implements TranslationActionFactory
{
	public static final String NAME = "mapIdentityByType";
	
	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescriptionKey()
	{
		return "TranslationAction.mapIdentityByType.desc";
	}

	@Override
	public ActionParameterDesc[] getParameters()
	{
		return new ActionParameterDesc[] {
				new ActionParameterDesc(
						true,
						"sourceIdentityType",
						"TranslationAction.mapIdentityByType.param.sourceIdentityType.desc",
						20),
				new ActionParameterDesc(
						true,
						"targetIdentityType",
						"TranslationAction.mapIdentityByType.param.targetIdentityType.desc",
						20),
				new ActionParameterDesc(
						true,
						"credentialRequirement",
						"TranslationAction.mapIdentityByType.param.credentialRequirement.desc",
						20) };
	}

	@Override
	public TranslationAction getInstance(String... parameters) throws EngineException
	{
		return new MapIdentityByTypeAction(parameters);
	}
}
