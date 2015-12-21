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

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((action == null) ? 0 : action.hashCode());
		result = prime * result + ((condition == null) ? 0 : condition.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AbstractTranslationRule<?> other = (AbstractTranslationRule<?>) obj;
		if (action == null)
		{
			if (other.action != null)
				return false;
		} else if (!action.equals(other.action))
			return false;
		if (condition == null)
		{
			if (other.condition != null)
				return false;
		} else if (!condition.equals(other.condition))
			return false;
		return true;
	}
}
