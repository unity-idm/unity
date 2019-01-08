/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.notifications.sms;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;
import pl.edu.icm.unity.base.utils.Log;

/**
 * Configuration of the selected SMS delivery serivce.
 * 
 * @author K. Benedyczak
 */
public class SMSServiceProperties extends PropertiesHelper
{
	private static final Logger log = Log.getLegacyLogger(Log.U_SERVER_CFG,
			SMSServiceProperties.class);
	
	@DocumentationReferencePrefix
	public static final String P = "unity.sms.";
	
	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> META = new HashMap<>();
	
	public enum Provider 
	{
		clickatell
	};
	
	public enum Charset {
		ASCII, 
		UCS2_BE, 
		UTF_8, 
		Windows_1252;

		public String toString()
		{
			return super.toString().replace('_', '-');
		}
	};
	
	public static final String SERVICE_PROVIDER = "provider";

	public static final String CLICKATELL = Provider.clickatell.name() + ".";
	public static final String CLICKATELL_API_KEY = CLICKATELL + "apiKey";
	public static final String CLICKATELL_CHARSET = CLICKATELL + "charset";
	
	static
	{
		META.put(SERVICE_PROVIDER, new PropertyMD(Provider.clickatell).
				setDescription("Controls the SMS gateway service providers"));
		
		META.put(CLICKATELL_API_KEY, new PropertyMD().
				setDescription("API key of the Clickatell service. "
						+ "Mandatory when using Clickatell provider."));
		META.put(CLICKATELL_CHARSET, new PropertyMD(Charset.UTF_8).
				setDescription("Message charset. ASCII, UCS2-BE, UTF-8 Windows-1252"));
	}
	
	public SMSServiceProperties(Properties properties)
	{
		super(P, properties, META, log);
		
		if (getEnumValue(SERVICE_PROVIDER, Provider.class) == Provider.clickatell
				&& !isSet(CLICKATELL_API_KEY))
			throw new ConfigurationException("The API key must be set for the Clickatell SMS service provider");
			
	}
}
