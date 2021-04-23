package io.imunity.upman;

import pl.edu.icm.unity.engine.api.config.UnityPropertiesHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.Logger;

import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertyMD;
import pl.edu.icm.unity.base.utils.Log;

/**
 * Configuration of the upman endpoint.
 * 
 * @author P.Piernik
 *
 */
public class UpmanEndpointProperties extends UnityPropertiesHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_UPMAN, UpmanEndpointProperties.class);

	@DocumentationReferencePrefix
	public static final String PREFIX = "unity.upman.";

	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> META = new HashMap<String, PropertyMD>();

	public static final String ENABLE_HOME_LINK = "enableHomeLink";
	public static final String HOME_ENDPOINT = "homeEndpoint";

	static
	{
		META.put(ENABLE_HOME_LINK, new PropertyMD("true")
				.setDescription("If true then the home service link is shown in header of Upman UI."));
		META.put(HOME_ENDPOINT, new PropertyMD().setDescription(
				"If home link is active, then link redirect to this service address. By default first active home service is used."));
	}

	public UpmanEndpointProperties(Properties properties) throws ConfigurationException
	{
		super(PREFIX, properties, META, log);
	}

	public boolean isHomeIsEnabled()
	{
		return getBooleanValue(ENABLE_HOME_LINK);
	}

	public String getHomeEndpoint()
	{
		return getValue(HOME_ENDPOINT);
	}
}
