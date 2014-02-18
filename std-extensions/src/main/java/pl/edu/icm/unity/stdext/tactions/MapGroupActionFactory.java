/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.tactions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.authn.remote.translation.ActionParameterDesc;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationAction;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationActionFactory;
import pl.edu.icm.unity.server.utils.UnityMessageSource;

/**
 * Factory for {@link MapGroupAction}.
 *   
 * @author K. Benedyczak
 */
@Component
public class MapGroupActionFactory implements TranslationActionFactory
{
	public static final String NAME = "mapGroup";
	private UnityMessageSource msg;
		
	@Autowired
	public MapGroupActionFactory(UnityMessageSource msg)
	{
		this.msg = msg;
	}
	
	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescription()
	{
		return msg.getMessage("TranslationAction.mapGroup.desc");
	}

	@Override
	public ActionParameterDesc[] getParameters()
	{
		return new ActionParameterDesc[] {
				new ActionParameterDesc(
						true,
						msg.getMessage("TranslationAction.mapGroup.param.1.name"),
						msg.getMessage("TranslationAction.mapGroup.param.1.desc"),
						20),
				new ActionParameterDesc(
						true,
						msg.getMessage("TranslationAction.mapGroup.param.2.name"),
						msg.getMessage("TranslationAction.mapGroup.param.2.desc"),
						20)};
	}

	@Override
	public TranslationAction getInstance(String... parameters) throws EngineException
	{
		return new MapGroupAction(parameters);
	}
}
