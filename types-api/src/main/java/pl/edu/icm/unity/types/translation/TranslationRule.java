/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.translation;

/**
 * Stores information about a pair: condition and {@link TranslationAction}.
 * 
 * @author Krzysztof Benedyczak
 */
public class TranslationRule
{
	protected String condition;
	protected TranslationAction action;

	public TranslationRule(String condition, TranslationAction action)
	{
		this.condition = condition;
		this.action = action;
	}

	protected TranslationRule(TranslationRule orig)
	{
		this.condition = orig.condition; 
		this.action = orig.action;
	}

	public TranslationRule()
	{
	}
	
	public String getCondition()
	{
		return condition;
	}
	
	public void setCondition(String condition)
	{
		this.condition = condition;
	}
	
	public TranslationAction getAction()
	{
		return action;
	}
	
	public void setTranslationAction(TranslationAction action)
	{
		this.action = action;
	}

	@Override
	public String toString()
	{
		return "TranslationRule [condition=" + condition + ", action=" + action + "]";
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
		TranslationRule other = (TranslationRule) obj;
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
