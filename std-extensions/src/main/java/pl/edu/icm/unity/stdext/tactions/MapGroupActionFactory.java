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
 * Factory for {@link MapGroupAction}.
 *   
 * @author K. Benedyczak
 */
@Component
public class MapGroupActionFactory implements TranslationActionFactory
{
	public static final String NAME = "mapGroup";
	
	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescriptionKey()
	{
		return "TranslationAction.mapGroup.desc";
	}

	@Override
	public ActionParameterDesc[] getParameters()
	{
		return new ActionParameterDesc[] {
				new ActionParameterDesc(
						true,
						"replaced",
						"TranslationAction.mapGroup.param.replaced.desc",
						20),
				new ActionParameterDesc(
						true,
						"replacement",
						"TranslationAction.mapGroup.param.replacement.desc",
						20)};
	}

	@Override
	public TranslationAction getInstance(String... parameters) throws EngineException
	{
		return new MapGroupAction(parameters);
	}
}
