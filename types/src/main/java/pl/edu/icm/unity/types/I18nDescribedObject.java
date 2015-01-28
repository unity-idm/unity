/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types;

import java.util.Locale;
import java.util.Map;

import org.springframework.context.NoSuchMessageException;

import pl.edu.icm.unity.MessageSource;

/**
 * Defines displayed name and description, both with a possibility to be translated
 * @author K. Benedyczak
 */
public class I18nDescribedObject
{
	protected I18nString displayedName;
	protected I18nString description;

	public I18nDescribedObject()
	{
	}

	public I18nDescribedObject(I18nString displayedName, I18nString description)
	{
		setDisplayedName(displayedName);
		setDescription(description);
	}

	public I18nString getDisplayedName()
	{
		return displayedName;
	}
	
	public void setDisplayedName(I18nString displayedName)
	{
		if (displayedName == null)
			throw new IllegalArgumentException("displayed name must not be null");
		this.displayedName = displayedName;
	}
	
	public I18nString getDescription()
	{
		return description;
	}
	
	public void setDescription(I18nString description)
	{
		this.description = description;
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
	public static I18nString loadI18nStringFromBundle(String key, MessageSource msg, Object... args)
	{
		Map<String, Locale> allLocales = msg.getSupportedLocales();
		I18nString ret = new I18nString();
		
		String defaultMessage;
		try
		{
			defaultMessage = msg.getMessage(key, args);
		} catch (NoSuchMessageException e)
		{
			return ret;
		}
		
		for (Locale locE: allLocales.values())
		{
			String message = msg.getMessage(key, args, locE);
			if (locE.toString().equals(msg.getDefaultLocaleCode()) || !defaultMessage.equals(message))
				ret.addValue(locE.toString(), message);
		}
		return ret;
	}
	
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((displayedName == null) ? 0 : displayedName.hashCode());
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
		I18nDescribedObject other = (I18nDescribedObject) obj;
		if (description == null)
		{
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (displayedName == null)
		{
			if (other.displayedName != null)
				return false;
		} else if (!displayedName.equals(other.displayedName))
			return false;
		return true;
	}
}
