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
 * Factory for {@link FilterIdentityAction}.
 *   
 * @author K. Benedyczak
 */
@Component
public class FilterIdentityActionFactory implements TranslationActionFactory
{
	public static final String NAME = "filterIdentity";
	
	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescriptionKey()
	{
		return "TranslationAction.filterIdentity.desc";
	}

	@Override
	public ActionParameterDesc[] getParameters()
	{
		return new ActionParameterDesc[] {
				new ActionParameterDesc(
						"identity",
						"TranslationAction.filterIdentity.paramDesc.idType",
						0, 1, Type.UNITY_ID_TYPE),
				new ActionParameterDesc(
						"identityValueRegexp",
						"TranslationAction.filterIdentity.paramDesc.idValueReqexp",
						0, 1, Type.EXPRESSION)};
	}

	@Override
	public FilterIdentityAction getInstance(String... parameters) throws EngineException
	{
		return new FilterIdentityAction(parameters, this);
	}

	@Override
	public ProfileType getSupportedProfileType()
	{
		return ProfileType.OUTPUT;
	}
}
