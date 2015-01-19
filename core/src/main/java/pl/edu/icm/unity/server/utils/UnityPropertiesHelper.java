/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.utils;

import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.types.I18nString;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;

/**
 * Extends {@link PropertiesHelper} with Unity specific methods. Currently only one - returning localized strings.
 * @author K. Benedyczak
 */
public class UnityPropertiesHelper extends PropertiesHelper
{

	public UnityPropertiesHelper(String prefix, Properties properties,
			Map<String, PropertyMD> propertiesMD, Logger log)
			throws ConfigurationException
	{
		super(prefix, properties, propertiesMD, log);
	}
	
	/**
	 * @param msg
	 * @param baseKey Property name (without the prefix).
	 * @return localized string of the given property. Default value is set to the regular value of the property.
	 * All sub values of the base key are used as localized values, assuming their subkey is equal to one of
	 * supported locales.
	 */
	public I18nString getLocalizedString(UnityMessageSource msg, String baseKey)
	{
		I18nString ret = new I18nString();
		Map<String, Locale> supportedLocales = msg.getSupportedLocales();
		String defaultVal = getValue(baseKey);
		if (defaultVal != null)
			ret.setDefaultValue(defaultVal);
		for (Map.Entry<String, Locale> locale: supportedLocales.entrySet())
		{
			String v = getLocalizedValue(baseKey, locale.getValue());
			if (v != null && !v.equals(defaultVal))
				ret.addValue(locale.getKey(), v);
		}
		return ret;
	}

}
