/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation;

import pl.edu.icm.unity.types.translation.TranslationAction;
import pl.edu.icm.unity.types.translation.TranslationActionType;


/**
 * Minimal base for translation action instance, configured to be executed.
 * @author K. Benedyczak
 */
public abstract class TranslationActionInstance extends TranslationAction
{
	protected TranslationActionType actionType;
	
	public TranslationActionInstance(TranslationActionType actionType, String[] parameters)
	{
		super(actionType.getName(), parameters);
		this.actionType = actionType;
	}

	public TranslationActionType getActionType()
	{
		return actionType;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((actionType == null) ? 0 : actionType.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		TranslationActionInstance other = (TranslationActionInstance) obj;
		if (actionType == null)
		{
			if (other.actionType != null)
				return false;
		} else if (!actionType.equals(other.actionType))
			return false;
		return true;
	}
}
