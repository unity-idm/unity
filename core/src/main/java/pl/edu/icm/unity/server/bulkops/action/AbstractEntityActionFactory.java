/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.bulkops.action;

import pl.edu.icm.unity.server.bulkops.EntityAction;
import pl.edu.icm.unity.server.translation.ActionParameterDesc;
import pl.edu.icm.unity.server.translation.ProfileType;
import pl.edu.icm.unity.server.translation.TranslationActionFactory;

/**
 * Boilerplate code for the {@link TranslationActionFactory} implementations producing {@link EntityAction}s.
 * @author K. Benedyczak
 */
public abstract class AbstractEntityActionFactory implements TranslationActionFactory
{
	private final String name;
	private final ActionParameterDesc[] parameters;
	
	public AbstractEntityActionFactory(String name, ActionParameterDesc... parameters)
	{
		this.name = name;
		this.parameters = parameters;
	}

	@Override
	public ProfileType getSupportedProfileType()
	{
		return ProfileType.BULK_ENTITY_OPS;
	}

	@Override
	public String getDescriptionKey()
	{
		return "EntityAction." + name + ".desc";
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public ActionParameterDesc[] getParameters()
	{
		return parameters;
	}
}
