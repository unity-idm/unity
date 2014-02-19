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
 * Factory for {@link MapAttributeToIdentityAction}.
 *   
 * @author K. Benedyczak
 */
@Component
public class MapIdentityByTypeActionFactory implements TranslationActionFactory
{
	public static final String NAME = "mapIdentityByType";
	private UnityMessageSource msg;
	
	@Autowired
	public MapIdentityByTypeActionFactory(UnityMessageSource msg)
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
		return msg.getMessage("TranslationAction.mapIdentityByType.desc");
	}

	@Override
	public ActionParameterDesc[] getParameters()
	{
		return new ActionParameterDesc[] {
				new ActionParameterDesc(
						true,
						msg.getMessage("TranslationAction.mapIdentityByType.param.1.name"),
						msg.getMessage("TranslationAction.mapIdentityByType.param.1.desc"),
						20),
				new ActionParameterDesc(
						true,
						msg.getMessage("TranslationAction.mapIdentityByType.param.2.name"),
						msg.getMessage("TranslationAction.mapIdentityByType.param.2.desc"),
						20),
				new ActionParameterDesc(
						true,
						msg.getMessage("TranslationAction.mapIdentityByType.param.3.name"),
						msg.getMessage("TranslationAction.mapIdentityByType.param.3.desc"),
						20) };
	}

	@Override
	public TranslationAction getInstance(String... parameters) throws EngineException
	{
		return new MapIdentityByTypeAction(parameters);
	}
}
