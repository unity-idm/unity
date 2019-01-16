/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation;

import pl.edu.icm.unity.engine.api.translation.TranslationActionInstance;
import pl.edu.icm.unity.engine.api.translation.TranslationCondition;
import pl.edu.icm.unity.types.translation.TranslationRule;

/**
 * Pair: condition and action; configured.
 *  
 * @author K. Benedyczak
 */
public abstract class TranslationRuleInstance<T extends TranslationActionInstance> extends TranslationRule
{
	protected T actionInstance;
	protected TranslationCondition conditionInstance;
	
	public TranslationRuleInstance(T action, TranslationCondition condition)
	{
		super(condition.getCondition(), action);
		this.actionInstance = action;
		this.conditionInstance = condition;
	}
	
	public T getActionInstance()
	{
		return actionInstance;
	}

	public TranslationCondition getConditionInstance()
	{
		return conditionInstance;
	}
}
