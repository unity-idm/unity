/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.bulkops.action;

import pl.edu.icm.unity.server.bulkops.EntityAction;
import pl.edu.icm.unity.server.bulkops.EntityActionFactory;
import pl.edu.icm.unity.server.translation.TranslationActionFactory;
import pl.edu.icm.unity.server.translation.TranslationActionInstance;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationActionType;

/**
 * Boilerplate code for the {@link TranslationActionFactory} implementations producing {@link EntityAction}s.
 * @author K. Benedyczak
 */
public abstract class AbstractEntityActionFactory implements EntityActionFactory
{
	private TranslationActionType actionType;
	
	public AbstractEntityActionFactory(String name, ActionParameterDefinition... parameters)
	{
		actionType = new TranslationActionType(ProfileType.BULK_ENTITY_OPS,
				"EntityAction." + name + ".desc",
				name,
				parameters);
	}

	@Override
	public TranslationActionType getActionType()
	{
		return actionType;
	}

	@Override
	public TranslationActionInstance getBlindInstance(String... parameters)
	{
		return new BlindStopperEntityAction(getActionType(), parameters);
	}
}
