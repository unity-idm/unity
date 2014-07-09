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
import pl.edu.icm.unity.server.translation.out.OutputTranslationAction;

/**
 * Factory for {@link FilterAttributeValuesAction}.
 *   
 * @author K. Benedyczak
 */
@Component
public class FilterAttributeValuesActionFactory implements TranslationActionFactory
{
	public static final String NAME = "filterAttributeValues";
	
	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescriptionKey()
	{
		return "TranslationAction.filterAttributeValues.desc";
	}

	@Override
	public ActionParameterDesc[] getParameters()
	{
		return new ActionParameterDesc[] {
				new ActionParameterDesc(
						"attribute",
						"TranslationAction.filterAttributeValue.paramDesc.attribute",
						1, 1, Type.UNITY_ATTRIBUTE),
				new ActionParameterDesc(
						"attributeValueRegexp",
						"TranslationAction.filterAttributeValue.paramDesc.attributeValueRegexp",
						1, 1, Type.EXPRESSION)
		};
	}

	@Override
	public OutputTranslationAction getInstance(String... parameters) throws EngineException
	{
		return new FilterAttributeValuesAction(parameters, this);
	}

	@Override
	public ProfileType getSupportedProfileType()
	{
		return ProfileType.OUTPUT;
	}
}
