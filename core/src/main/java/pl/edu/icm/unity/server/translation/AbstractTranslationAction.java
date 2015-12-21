/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation;

import java.util.Arrays;


/**
 * Minimal base for translation actions.
 * @author K. Benedyczak
 */
public abstract class AbstractTranslationAction implements TranslationAction
{
	protected TranslationActionDescription description;
	protected String[] params;
	
	public AbstractTranslationAction(TranslationActionDescription description, String[] params)
	{
		this.description = description;
		this.params = params;
	}

	@Override
	public TranslationActionDescription getActionDescription()
	{
		return description;
	}
	
	@Override
	public String[] getParameters()
	{
		return params;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((description == null) ? 0 : description.getName().hashCode());
		result = prime * result + ((description == null) ? 0 : description.getSupportedProfileType().hashCode());
		result = prime * result + Arrays.hashCode(params);
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
		AbstractTranslationAction other = (AbstractTranslationAction) obj;
		if (description == null)
		{
			if (other.description != null)
				return false;
		} else
		{
			if (!description.getName().equals(other.description.getName()))
				return false;
			if (!description.getSupportedProfileType().equals(other.description.getSupportedProfileType()))
				return false;
		}
		if (!Arrays.equals(params, other.params))
			return false;
		return true;
	}
	
	
}
