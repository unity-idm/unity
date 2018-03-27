/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.translation;

import java.util.stream.Stream;

import pl.edu.icm.unity.types.translation.ActionParameterDefinition;
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
		checkParams();
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

	protected void checkParams()
	{
		ActionParameterDefinition[] paramDef = getActionType().getParameters();
		if (paramDef == null)
			paramDef = new ActionParameterDefinition[0];
		String[] paramVal = getParameters();
		if (paramVal == null)
			paramVal = new String[0];
		
		int mandatorySize = Long
				.valueOf(Stream.of(paramDef).filter(d -> d.isMandatory()).count())
				.intValue();
		int defSize = paramDef.length;
		int valSize = paramVal.length;

		if (valSize < mandatorySize)
		{
			if (mandatorySize < defSize)
			{
				throw new IllegalArgumentException(

						"Action require min " + defSize + " parameters");
			} else
			{
				throw new IllegalArgumentException(
						"Action require exacly " + defSize + " parameters");
			}
		}

		if (valSize > defSize)
		{
			if (defSize == 0)
			{
				throw new IllegalArgumentException("Action requires no parameters");
				
			} else
			{
				throw new IllegalArgumentException(
						"Action require max " + defSize + " parameters");
			}

		}
		
		for (int i = 0; i < defSize; i++)
		{
			if (paramDef[i].isMandatory() && (i >= valSize  || paramVal[i] == null || paramVal[i].isEmpty()))
			{
				throw new IllegalArgumentException("Action requires "
						+ paramDef[i].getName() + " parameter");
			}
		}
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
