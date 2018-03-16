/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.bulkops;

import pl.edu.icm.unity.engine.api.translation.TranslationActionInstance;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.translation.TranslationActionType;

/**
 * Implementation performs an action on a given entity.
 * @author K. Benedyczak
 */
public abstract class EntityAction extends TranslationActionInstance
{
	public EntityAction(TranslationActionType actionType, String[] parameters)
	{
		this(actionType, parameters, true);
	}

	public EntityAction(TranslationActionType actionType, String[] parameters,
			boolean checkParams)
	{
		super(actionType, parameters, checkParams);
	}
	
	/**
	 * Performs an implementation specific action.
	 * @param entity entity to operate on.
	 */
	public abstract void invoke(Entity entity);
}
