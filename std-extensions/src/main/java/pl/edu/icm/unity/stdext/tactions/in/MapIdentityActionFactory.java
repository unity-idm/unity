/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.tactions.in;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.translation.ActionParameterDesc;
import pl.edu.icm.unity.server.translation.ActionParameterDesc.Type;
import pl.edu.icm.unity.server.translation.ProfileType;
import pl.edu.icm.unity.server.translation.TranslationActionFactory;
import pl.edu.icm.unity.server.translation.in.IdentityEffectMode;
import pl.edu.icm.unity.server.translation.in.InputTranslationAction;

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
	public InputTranslationAction getInstance(String... parameters) throws EngineException
	{
		return new MapIdentityAction(parameters, this);
	}

	@Override
	public ProfileType getSupportedProfileType()
	{
		return ProfileType.INPUT;
	}
}
