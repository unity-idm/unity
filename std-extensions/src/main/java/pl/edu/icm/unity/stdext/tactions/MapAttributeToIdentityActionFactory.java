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
public class MapAttributeToIdentityActionFactory implements TranslationActionFactory
{
	public static final String NAME = "mapAttributeToIdentity";
	
	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescriptionKey()
	{
		return "TranslationAction.mapAttributeToIdentity.desc";
	}

	@Override
	public ActionParameterDesc[] getParameters()
	{
		return new ActionParameterDesc[] {
				new ActionParameterDesc(
						true,
						"attribute",
						"TranslationAction.mapAttributeToIdentity.param.attribute.desc",
						20),
				new ActionParameterDesc(
						true,
						"identityType",
						"TranslationAction.mapAttributeToIdentity.param.identityType.desc",
						20),
				new ActionParameterDesc(
						true,
						"credentialRequirement",
						"TranslationAction.mapAttributeToIdentity.param.credentialRequirement.desc",
						20) };
	}

	@Override
	public TranslationAction getInstance(String... parameters) throws EngineException
	{
		return new MapAttributeToIdentityAction(parameters);
	}
}
