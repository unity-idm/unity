/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.config;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import eu.unicore.util.configuration.FilePropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;

/**
 * Extends {@link FilePropertiesHelper} with Unity specific methods. Currently only one - returning localized strings.
 * Except of the base class the same as {@link UnityPropertiesHelper}. 
 * @author K. Benedyczak
 */
public class UnityFilePropertiesHelper extends FilePropertiesHelper
{
	public UnityFilePropertiesHelper(String prefix, File file, Map<String, PropertyMD> meta,
			Logger log) throws IOException
	{
		super(prefix, file, meta, log);
	}

	public UnityFilePropertiesHelper(String prefix, String file, Map<String, PropertyMD> meta,
			Logger log) throws IOException
	{
		super(prefix, file, meta, log);
	}

	/**
	 * @param msg
	 * @param baseKey Property name (without the prefix).
	 * @return localized string of the given property. Default value is set to the regular value of the property.
	 * All sub values of the base key are used as localized values, assuming their subkey is equal to one of
	 * supported locales.
	 */
	public I18nString getLocalizedString(MessageSource msg, String baseKey)
	{
		return UnityPropertiesHelper.getLocalizedString(this, msg, baseKey);
	}
}
