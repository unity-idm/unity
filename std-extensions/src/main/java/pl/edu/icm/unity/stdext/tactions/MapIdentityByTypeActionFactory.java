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
	
	private static final ActionParameterDesc[] PARAMS = {
		new ActionParameterDesc(true, "sourceIdentityType", 
				"Identity type to be mapped.", 20),
		new ActionParameterDesc(true, "targetIdentityType", 
				"Target unity name of identity type to be used instead of the original one.", 20),
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
		return "Maps a remote identity by type to a local identity with a new type.";
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
