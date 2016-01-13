/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.bulkops;

import pl.edu.icm.unity.server.translation.TranslationAction;
import pl.edu.icm.unity.types.basic.Entity;

/**
 * Implementation performs an action on a given entity.
 * @author K. Benedyczak
 */
public interface EntityAction extends TranslationAction
{
	/**
	 * Performs an implementation specific action.
	 * @param entity entity to operate on.
	 */
	void invoke(Entity entity);
}
