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

	static
	{
		META.put(ROOT_GROUP, new PropertyMD()
				.setDescription("Root group."));
		META.put(AUTHORIZATION_GROUP, new PropertyMD().setDescription(
				"Group where ProjectManagementRESTAPIRole will be check."));
	}

	public UpmanRestEndpointProperties(Properties properties) throws ConfigurationException
	{
		super(PREFIX, properties, META, log);
	}
}
