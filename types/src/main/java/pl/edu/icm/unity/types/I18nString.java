/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types;

import java.util.HashMap;
import java.util.Map;

import pl.edu.icm.unity.MessageSource;


/**
 * String in several languages. Besides localized versions can store also a default value which is returned when there
 * is no version for the requested locale and default locale. 
 * @author K. Benedyczak
 */
public class I18nString
{
	private Map<String, String> values;
	private String defaultValue;
	
	public I18nString()
	{
		this.values = new HashMap<String, String>();
	}

	public I18nString(String defaultValue)
	{
		this();
		this.defaultValue = defaultValue;
	}

	public String getValue(MessageSource msg)
	{
		return getValue(msg.getLocaleCode(), msg.getDefaultLocaleCode());
	}

	public String getValue(String locale, String defaultLocale)
	{
		return values.containsKey(locale) ? values.get(locale) : 
			values.containsKey(defaultLocale) ? values.get(defaultLocale) : defaultValue;
	}
	
	public void addValue(String locale, String value)
	{
		values.put(locale, value);
	}
	
	public void addAllValues(Map<String, String> values)
	{
		this.values.putAll(values);
	}
	
	public Map<String, String> getMap()
	{
		return new HashMap<String, String>(values);
	}

	public String getDefaultValue()
	{
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue)
	{
		this.defaultValue = defaultValue;
	}

	public boolean isEmpty()
	{
		return (defaultValue == null || defaultValue.isEmpty()) && values.isEmpty();
	}
	
	@Override
	public String toString()
	{
		return "I18nString [values=" + values + ", defaultValue=" + defaultValue + "]";
	}

	@Override
	public I18nString clone()
	{
		I18nString ret = new I18nString(defaultValue);
		ret.addAllValues(values);
		return ret;
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((defaultValue == null) ? 0 : defaultValue.hashCode());
		result = prime * result + ((values == null) ? 0 : values.hashCode());
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
		I18nString other = (I18nString) obj;
		if (defaultValue == null)
		{
			if (other.defaultValue != null)
				return false;
		} else if (!defaultValue.equals(other.defaultValue))
			return false;
		if (values == null)
		{
			if (other.values != null)
				return false;
		} else if (!values.equals(other.values))
			return false;
		return true;
	}
}
