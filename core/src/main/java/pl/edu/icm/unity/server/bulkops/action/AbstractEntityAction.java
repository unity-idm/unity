/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.bulkops.action;

import pl.edu.icm.unity.server.bulkops.EntityAction;
import pl.edu.icm.unity.server.translation.AbstractTranslationAction;
import pl.edu.icm.unity.server.translation.TranslationActionDescription;

/**
 * Simple base class for {@link EntityAction}s.
 * @author K. Benedyczak
 */
public abstract class AbstractEntityAction extends AbstractTranslationAction implements EntityAction
{
	public AbstractEntityAction(TranslationActionDescription description, String[] params)
	{
		super(description, params);
	}
}
