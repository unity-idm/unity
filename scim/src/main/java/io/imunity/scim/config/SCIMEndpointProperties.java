/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.Logger;

import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertyMD;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.config.UnityPropertiesHelper;

public class SCIMEndpointProperties extends UnityPropertiesHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CFG, SCIMEndpointProperties.class);

	@DocumentationReferencePrefix
	public static final String PREFIX = "unity.endpoint.scim.";

	public static final String ROOT_GROUP = "rootGroup";
	public static final String REST_ADMIN_GROUP = "restAdminGroup";
	public static final String MEMBERSHIP_GROUPS = "membershipGroups.";
	public static final String EXCLUDED_MEMBERSHIP_GROUPS = "exludedMembershipGroups.";
	public static final String MEMBERSHIP_ATTRIBUTES = "membershipAttributes.";
	public static final String SCHEMAS = "schemas.";
	public static final String SCHEMAS_FILE = "schemasFile.";

	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> META = new HashMap<>();

	static
	{
		META.put(ROOT_GROUP,
				new PropertyMD().setMandatory().setDescription("SCIM root group for attributes resolution"));
		META.put(REST_ADMIN_GROUP, new PropertyMD().setDescription(
				"Users in this group will be authorized to perform endpoint reconfigurations over the REST interface under /configuration."));
		META.put(MEMBERSHIP_GROUPS, new PropertyMD().setList(false).setDescription(
				"List of SCIM membership groups with wildcards supported. Only memberships in those groups (and their children) are exposed via SCIM."));
		META.put(EXCLUDED_MEMBERSHIP_GROUPS, new PropertyMD().setList(false).setDescription(
				"List of groups with wildcard supported that shouldn't be excluded from SCIM membership groups"));
		META.put(MEMBERSHIP_ATTRIBUTES,
				new PropertyMD().setList(false).setDescription("SCIM group membership attributes"));
		META.put(SCHEMAS, new PropertyMD().setList(false).setDescription("SCIM schemas definitions"));
		META.put(SCHEMAS_FILE, new PropertyMD().setList(false).setHidden().setDescription("SCIM schemas definitions from file"));
	}

	public SCIMEndpointProperties(Properties properties) throws ConfigurationException
	{
		super(PREFIX, properties, META, log);
	}
}
