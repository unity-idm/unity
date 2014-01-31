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
	
	private static final ActionParameterDesc[] PARAMS = {
		new ActionParameterDesc(true, "attribute", 
				"Value of attribute with this name will be used as a new identity value.", 20),
		new ActionParameterDesc(true, "identityType", 
				"A unity name of identity type to be used for the newly created identity.", 20),
		new ActionParameterDesc(true, "credentialRequirement", "Credential requirement to be used " +
				"for the identity in case a new entity is created from it.", 20)
	};
	
	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescription()
	{
		return "Maps a remote attribute to a local identity with a given type.";
	}

	@Override
	public ActionParameterDesc[] getParameters()
	{
		return PARAMS;
	}

	@Override
	public TranslationAction getInstance(String... parameters) throws EngineException
	{
		return new MapAttributeToIdentityAction(parameters);
	}
}
