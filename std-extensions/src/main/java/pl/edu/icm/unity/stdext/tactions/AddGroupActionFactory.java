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
 * Factory for {@link AddGroupAction}.
 *   
 * @author K. Benedyczak
 */
@Component
public class AddGroupActionFactory implements TranslationActionFactory
{
	public static final String NAME = "addGroup";
	
	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescriptionKey()
	{
		return "TranslationAction.addGroup.desc";
	}

	@Override
	public ActionParameterDesc[] getParameters()
	{
		return new ActionParameterDesc[] {
				new ActionParameterDesc(
						true,
						"group",
						"TranslationAction.addGroup.param.group.desc",
						20)};
	}

	@Override
	public TranslationAction getInstance(String... parameters) throws EngineException
	{
		return new AddGroupAction(parameters);
	}
}
