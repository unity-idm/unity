package io.imunity.attr.introspection.config;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.config.UnityPropertiesHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.Logger;

import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertyMD;

/**
 * Configuration of the attribute introspection endpoint.
 * 
 * @author P.Piernik
 *
 */
public class AttrIntrospectionEndpointProperties extends UnityPropertiesHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_ATTR_INTROSPECTION,
			AttrIntrospectionEndpointProperties.class);

	public static final String CUSTOM_ATTRIBUTE_POLICIES = "customAttributePolicies.";
	public static final String ATTRIBUTE_POLICY_NAME = "policyName";
	public static final String ATTRIBUTE_POLICY_TARGET_IDPS = "targetIdps";
	public static final String ATTRIBUTE_POLICY_TARGET_FEDERATIONS = "targetFederations";
	public static final String ATTRIBUTE_POLICY_ATTRIBUTES = "attributes.";
	public static final String DEFAULT_POLICY_ATTRIBUTES = "defaultPolicyAttributes.";
	
	@DocumentationReferencePrefix
	public static final String PREFIX = "unity.attrintrospection.";

	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> META = new HashMap<>();

	static
	{
		
		META.put(DEFAULT_POLICY_ATTRIBUTES,
				new PropertyMD().setList(true).setDescription("List of default policy attributes"));
		META.put(CUSTOM_ATTRIBUTE_POLICIES,
				new PropertyMD().setStructuredList(true).setDescription("List of custom attribute policies"));
		META.put(ATTRIBUTE_POLICY_NAME, new PropertyMD().setStructuredListEntry(CUSTOM_ATTRIBUTE_POLICIES).setMandatory()
				.setDescription("Policy name"));
		META.put(ATTRIBUTE_POLICY_TARGET_IDPS, new PropertyMD().setStructuredListEntry(CUSTOM_ATTRIBUTE_POLICIES)
				.setDescription("Target idPs list separeted by ;"));
		META.put(ATTRIBUTE_POLICY_TARGET_FEDERATIONS, new PropertyMD().setStructuredListEntry(CUSTOM_ATTRIBUTE_POLICIES)
				.setDescription("Target federations list separeted by ;"));
		META.put(ATTRIBUTE_POLICY_ATTRIBUTES, new PropertyMD().setList(true).setStructuredListEntry(CUSTOM_ATTRIBUTE_POLICIES)
				.setDescription("List of custom policy attributes"));
	}

	public AttrIntrospectionEndpointProperties(Properties properties) throws ConfigurationException
	{
		super(PREFIX, properties, META, log);
	}

}
