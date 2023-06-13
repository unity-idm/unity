/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.bulkops.action;

import pl.edu.icm.unity.base.translation.ActionParameterDefinition;
import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.base.translation.TranslationActionType;
import pl.edu.icm.unity.engine.api.bulkops.EntityAction;
import pl.edu.icm.unity.engine.api.bulkops.EntityActionFactory;
import pl.edu.icm.unity.engine.api.translation.TranslationActionFactory;

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
	public EntityAction getBlindInstance(String... parameters)
	{
		return new BlindStopperEntityAction(getActionType(), parameters);
	}
}
