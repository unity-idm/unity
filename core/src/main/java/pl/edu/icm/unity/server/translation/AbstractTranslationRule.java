/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation;

/**
 * Pair: condition and action; configured.
 *  
 * @author K. Benedyczak
 */
public abstract class AbstractTranslationRule<T extends TranslationAction>
{
	protected T action;
	protected TranslationCondition condition;
	
	public AbstractTranslationRule(T action, TranslationCondition condition)
	{
		this.action = action;
		this.condition = condition;
	}
	
	public T getAction()
	{
		return action;
	}

	public void setAction(T action)
	{
		this.action = action;
	}

	public TranslationCondition getCondition()
	{
		return condition;
	}

	public void setCondition(TranslationCondition condition)
	{
		this.condition = condition;
	}
}
