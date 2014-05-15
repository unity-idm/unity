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
 * Factory for {@link AddAttributeAction}.
 *   
 * @author K. Benedyczak
 */
@Component
public class AddAttributeActionFactory implements TranslationActionFactory
{
	public static final String NAME = "addAttribute";
	
	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescriptionKey()
	{
		return "TranslationAction.addAttribute.desc";
	}

	@Override
	public ActionParameterDesc[] getParameters()
	{
		return new ActionParameterDesc[] {
				new ActionParameterDesc(
						true,
						"attribute",
						"TranslationAction.addAttribute.param.attribute.desc",
						20),
				new ActionParameterDesc(
						true,
						"group",
						"TranslationAction.addAttribute.param.group.desc",
						20),
				new ActionParameterDesc(
						false,
						"value",
						"TranslationAction.addAttribute.param.value.desc",
						20)};
	}

	@Override
	public TranslationAction getInstance(String... parameters) throws EngineException
	{
		return new AddAttributeAction(parameters);
	}
}
