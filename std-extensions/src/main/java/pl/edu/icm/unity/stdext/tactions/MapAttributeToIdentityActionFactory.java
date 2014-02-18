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
public class MapAttributeToIdentityActionFactory implements TranslationActionFactory
{
	public static final String NAME = "mapAttributeToIdentity";
	private UnityMessageSource msg;
	
	@Autowired
	public MapAttributeToIdentityActionFactory(UnityMessageSource msg)
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
		return msg.getMessage("TranslationAction.mapAttributeToIdentity.desc");
	}

	@Override
	public ActionParameterDesc[] getParameters()
	{
		return new ActionParameterDesc[] {
				new ActionParameterDesc(
						true,
						msg.getMessage("TranslationAction.mapAttributeToIdentity.param.1.name"),
						msg.getMessage("TranslationAction.mapAttributeToIdentity.param.1.desc"),
						20),
				new ActionParameterDesc(
						true,
						msg.getMessage("TranslationAction.mapAttributeToIdentity.param.2.name"),
						msg.getMessage("TranslationAction.mapAttributeToIdentity.param.2.desc"),
						20),
				new ActionParameterDesc(
						true,
						msg.getMessage("TranslationAction.mapAttributeToIdentity.param.3.name"),
						msg.getMessage("TranslationAction.mapAttributeToIdentity.param.3.desc"),
						20) };
	}

	@Override
	public TranslationAction getInstance(String... parameters) throws EngineException
	{
		return new MapAttributeToIdentityAction(parameters);
	}
}
