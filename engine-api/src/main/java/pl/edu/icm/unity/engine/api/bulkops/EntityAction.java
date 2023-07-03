/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.bulkops;

import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.translation.TranslationActionType;
import pl.edu.icm.unity.engine.api.translation.TranslationActionInstance;

/**
 * Implementation performs an action on a given entity.
 * @author K. Benedyczak
 */
public abstract class EntityAction extends TranslationActionInstance
{
	public EntityAction(TranslationActionType actionType, String[] parameters)
	{
		super(actionType, parameters);
	}
	
	/**
	 * Performs an implementation specific action.
	 * @param entity entity to operate on.
	 */
	public abstract void invoke(Entity entity);
}
