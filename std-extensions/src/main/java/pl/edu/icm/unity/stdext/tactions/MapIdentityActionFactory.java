/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.tactions;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.authn.remote.translation.ActionParameterDesc;
import pl.edu.icm.unity.server.authn.remote.translation.ActionParameterDesc.Type;
import pl.edu.icm.unity.server.authn.remote.translation.IdentityEffectMode;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationAction;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationActionFactory;

/**
 * Factory for identity mapping action.
 *   
 * @author K. Benedyczak
 */
@Component
public class MapIdentityActionFactory implements TranslationActionFactory
{
	public static final String NAME = "mapIdentity";
	
	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescriptionKey()
	{
		return "TranslationAction.mapIdentity.desc";
	}

	@Override
	public ActionParameterDesc[] getParameters()
	{
		return new ActionParameterDesc[] {
				new ActionParameterDesc(
						"unityIdentityType",
						"TranslationAction.mapIdentity.paramDesc.unityIdentityType",
						1, 1, Type.UNITY_ID_TYPE),
				new ActionParameterDesc(
						"expression",
						"TranslationAction.mapIdentity.paramDesc.expression",
						1, 1, Type.EXPRESSION),
				new ActionParameterDesc(
						"credential requirement",
						"TranslationAction.mapIdentity.paramDesc.credentialRequirement",
						1, 1, Type.UNITY_CRED_REQ),
				new ActionParameterDesc(
						"effect",
						"TranslationAction.mapIdentity.paramDesc.effect",
						1, 1, IdentityEffectMode.class)};
	}

	@Override
	public TranslationAction getInstance(String... parameters) throws EngineException
	{
		return new MapIdentityAction(parameters, this);
	}
}
