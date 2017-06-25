/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types;

import java.util.Arrays;

import pl.edu.icm.unity.MessageSource;


/**
 * Stores message key and arguments, to be resolved from message bundle.
 *  
 * @author K. Benedyczak
 */
public class I18nStringSource
{
	private String key;
	private Object[] arguments;
	
	public I18nStringSource(String key, Object... arguments)
	{
		this.key = key;
		this.arguments = arguments;
	}

	public String getValue(MessageSource msg)
	{
		return msg.getMessage(key, arguments);
	}

	public String getKey()
	{
		return key;
	}

	public Object[] getArguments()
	{
		return arguments;
	}

	@Override
	public String toString()
	{
		return "I18nStringSource [key=" + key + ", arguments=" + Arrays.toString(arguments)
				+ "]";
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(arguments);
		result = prime * result + ((key == null) ? 0 : key.hashCode());
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
		I18nStringSource other = (I18nStringSource) obj;
		if (!Arrays.equals(arguments, other.arguments))
			return false;
		if (key == null)
		{
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		return true;
	}
}
