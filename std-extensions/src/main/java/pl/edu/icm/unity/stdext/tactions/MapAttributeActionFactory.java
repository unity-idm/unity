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
 * Factory for {@link MapAttributeAction}.
 *   
 * @author K. Benedyczak
 */
@Component
public class MapAttributeActionFactory implements TranslationActionFactory
{
	public static final String NAME = "mapAttribute";
	private UnityMessageSource msg;
	
	
	@Autowired
	public MapAttributeActionFactory(UnityMessageSource msg)
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
		return msg.getMessage("TranslationAction.mapAttribute.desc");
	}

	@Override
	public ActionParameterDesc[] getParameters()
	{
		return  new ActionParameterDesc[] {
				new ActionParameterDesc(
						true,
						msg.getMessage("TranslationAction.mapAttribute.param.1.name"),
						msg.getMessage("TranslationAction.mapAttribute.param.1.desc"),
						20),
				new ActionParameterDesc(
						true,
						msg.getMessage("TranslationAction.mapAttribute.param.2.name"),
						msg.getMessage("TranslationAction.mapAttribute.param.2.desc"),
						20),
				new ActionParameterDesc(
						true,
						msg.getMessage("TranslationAction.mapAttribute.param.3.name"),
						msg.getMessage("TranslationAction.mapAttribute.param.3.desc"),
						20) };
	}

	@Override
	public TranslationAction getInstance(String... parameters) throws EngineException
	{
		return new MapAttributeAction(parameters);
	}
}
