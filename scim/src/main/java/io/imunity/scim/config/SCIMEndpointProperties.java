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
	public static final String MEMBERSHIP_GROUPS = "membershipGroups.";
	public static final String EXCLUDED_MEMBERSHIP_GROUPS = "exludedMembershipGroups.";
	public static final String MEMBERSHIP_ATTRIBUTES = "membershipAttributes.";

	public static final String SCHEMAS = "schemas.";

	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> META = new HashMap<>();

	static
	{
		META.put(ROOT_GROUP,
				new PropertyMD().setMandatory().setDescription("SCIM root group for attributes resolution"));
		META.put(MEMBERSHIP_GROUPS, new PropertyMD().setList(false).setDescription(
				"SCIM membership groups. Only memberships in those groups (and their children) are exposed via SCIM"));
		META.put(EXCLUDED_MEMBERSHIP_GROUPS, new PropertyMD().setList(false).setDescription(
				"List of groups that shouldn't be excluded from SCIM membership groups"));
		META.put(MEMBERSHIP_ATTRIBUTES, new PropertyMD().setList(false).setDescription(
				"SCIM group membership attributes"));
		META.put(SCHEMAS, new PropertyMD().setList(false).setDescription("SCIM schemas definitions"));
	}

	public SCIMEndpointProperties(Properties properties) throws ConfigurationException
	{
		super(PREFIX, properties, META, log);
	}
}
