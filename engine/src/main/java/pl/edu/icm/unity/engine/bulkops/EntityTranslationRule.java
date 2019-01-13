/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.bulkops;

import pl.edu.icm.unity.engine.api.bulkops.EntityAction;
import pl.edu.icm.unity.engine.api.translation.TranslationCondition;
import pl.edu.icm.unity.engine.translation.TranslationRuleInstance;

/**
 * For more concise notation
 * @author K. Benedyczak
 */
public class EntityTranslationRule extends TranslationRuleInstance<EntityAction>
{
	public EntityTranslationRule(EntityAction action, TranslationCondition condition)
	{
		super(action, condition);
	}
}
