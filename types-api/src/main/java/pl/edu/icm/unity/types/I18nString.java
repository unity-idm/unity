/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.context.NoSuchMessageException;

import pl.edu.icm.unity.MessageSource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;


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
		this.values = new HashMap<>();
	}

	public I18nString(String defaultValue)
	{
		this();
		this.defaultValue = defaultValue;
	}

	public I18nString(String locale, String value)
	{
		this();
		if (locale == null)
			this.defaultValue = value;
		else
			this.values.put(locale, value);
	}
	
	/**
	 * Loads {@link I18nString} from all message bundles which are installed in the system. The returned object 
	 * has no default value set.
	 * @param prefix
	 * @param name
	 * @param suffix
	 * @param msg
	 * @param msgKey
	 * @param args
	 * @return
	 */
	public I18nString(String key, MessageSource msg, Object... args)
	{
		this();
		Map<String, Locale> allLocales = msg.getSupportedLocales();
		
		String defaultMessage;
		try
		{
			defaultMessage = msg.getMessageUnsafe(key, args);
		} catch (NoSuchMessageException e)
		{
			return;
		}
		
		for (Locale locE: allLocales.values())
		{
			String message = msg.getMessage(key, args, locE);
			if (locE.toString().equals(msg.getDefaultLocaleCode()) || !defaultMessage.equals(message))
				addValue(locE.toString(), message);
		}
	}
	
	@JsonCreator
	public static I18nString fromJson(JsonNode json)
	{
		return I18nStringJsonUtil.fromJson(json);
	}
	
	@JsonValue
	public JsonNode toJson()
	{
		return I18nStringJsonUtil.toJson(this);
	}
	
	public String getValue(MessageSource msg)
	{
		return getValue(msg.getLocaleCode(), msg.getDefaultLocaleCode());
	}

	public String getValue(String locale, String defaultLocale)
	{
		return (locale != null && values.containsKey(locale)) ? values.get(locale) : 
			(defaultLocale != null && values.containsKey(defaultLocale)) ? values.get(defaultLocale) : 
			defaultValue;
	}

	public String getValueRaw(String locale)
	{
		return values.get(locale);
	}
	
	public void addValue(String locale, String value)
	{
		if (locale != null)
			values.put(locale, value);
		else
			setDefaultValue(value);
	}
	
	public void addAllValues(Map<String, String> values)
	{
		this.values.putAll(values);
	}
	
	public Map<String, String> getMap()
	{
		return new HashMap<String, String>(values);
	}

	public String getDefaultLocaleValue(MessageSource msg)
	{
		return getValue(null, msg.getDefaultLocaleCode());
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
