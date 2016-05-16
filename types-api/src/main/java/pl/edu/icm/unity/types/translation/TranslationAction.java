/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.translation;

import java.util.Arrays;

/**
 * Describes a configured translation action. Basically action name with its parameters.
 * 
 * @author Krzysztof Benedyczak
 */
public class TranslationAction
{
	private String name;
	private String[] parameters;

	public TranslationAction(String name, String[] parameters)
	{
		this.name = name;
		this.parameters = parameters;
	}

	public String getName()
	{
		return name;
	}
	public String[] getParameters()
	{
		return parameters;
	}

	@Override
	public String toString()
	{
		return "TranslationAction [action=" + name + ", parameters="
				+ Arrays.toString(parameters) + "]";
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + Arrays.hashCode(parameters);
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
		TranslationAction other = (TranslationAction) obj;
		if (name == null)
		{
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (!Arrays.equals(parameters, other.parameters))
			return false;
		return true;
	}
}
