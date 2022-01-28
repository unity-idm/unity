/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.PropertyMD;
import pl.edu.icm.unity.rest.RESTEndpointProperties;

public class SCIMEndpointProperties extends RESTEndpointProperties
{
	public static final String PREFIX = "unity.endpoint.scim.";

	public static final String ROOT_GROUP = "rootGroup";
	public static final String MEMBERSHIP_GROUPS = "membershipGroups";

	public static Map<String, PropertyMD> getDefaults()
	{
		Map<String, PropertyMD> defaults = new HashMap<>();

		defaults.put(ROOT_GROUP,
				new PropertyMD().setMandatory().setDescription("SCIM root group for attributes resolution"));
		defaults.put(MEMBERSHIP_GROUPS, new PropertyMD().setList(false).setDescription(
				"SCIM membership groups. Only memberships in those groups (and their children) are exposed via SCIM"));

		defaults.putAll(RESTEndpointProperties.getDefaults());
		return defaults;
	}

	public SCIMEndpointProperties(Properties properties) throws ConfigurationException
	{
		super(PREFIX, getDefaults(), properties);
	}

}
