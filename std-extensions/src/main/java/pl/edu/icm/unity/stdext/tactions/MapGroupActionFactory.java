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
	
	private static final ActionParameterDesc[] PARAMS = {
		new ActionParameterDesc(true, "replaced", 
				"Groups which have a name matching this regular expression will be mapped.", 20),
		new ActionParameterDesc(true, "replacement", 
				"A unity name of a group, may contain references to regexp groups from the first parameter.", 20)
	};
	
	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescription()
	{
		return "Maps a remote group to a local group.";
	}

	@Override
	public ActionParameterDesc[] getParameters()
	{
		return PARAMS;
	}

	@Override
	public TranslationAction getInstance(String... parameters) throws EngineException
	{
		return new MapGroupAction(parameters);
	}
}
