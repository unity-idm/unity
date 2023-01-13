package io.imunity.upman.rest;

import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertyMD;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.config.UnityPropertiesHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class UpmanRestEndpointProperties extends UnityPropertiesHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_UPMAN, UpmanRestEndpointProperties.class);

	@DocumentationReferencePrefix
	public static final String PREFIX = "unity.upman.rest.";

	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> META = new HashMap<>();

	public static final String ROOT_GROUP = "rootGroup";
	public static final String AUTHORIZATION_GROUP = "authorizationGroup";
	public static final String ENABLED_CORS_ORIGINS = "allowedCorsOrigins.";
	public static final String ENABLED_CORS_HEADERS = "allowedCorsHeaders.";

	static
	{
		META.put(ROOT_GROUP, new PropertyMD()
				.setDescription("Root group."));
		META.put(AUTHORIZATION_GROUP, new PropertyMD().setDescription(
				"Group where ProjectManagementRESTAPIRole will be check."));
		META.put(ENABLED_CORS_ORIGINS, new PropertyMD().setList(false).setDescription(
			"List of origins allowed for the CORS requests. "
				+ "The complete set of HTTP methods is enabled for the enumerated resources. "
				+ "If the list is undefined then CORS support is turned off."));
		META.put(ENABLED_CORS_HEADERS, new PropertyMD().setList(false).setDescription(
			"List of headers allowed for the CORS requests. If undefined then all are enabled by defult."));
	}

	public UpmanRestEndpointProperties(Properties properties) throws ConfigurationException
	{
		super(PREFIX, properties, META, log);
	}
}
