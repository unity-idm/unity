/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.to3_12;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;

import pl.edu.icm.unity.Constants;

public class UpdateHelperTo17
{
	public final static Set<String> oauthTokenTypes = Sets.newHashSet("oauth2Code", "oauth2Access", "oauth2Refresh",
			"usedOauth2Refresh");

	public static Optional<ObjectNode> fixOauthToken(ObjectNode objContent)
	{
		if (objContent.has("audience"))
		{
			JsonNode jsonNode = objContent.get("audience");
			if (jsonNode.isArray())
				return Optional.empty();
			ArrayNode audience = Constants.MAPPER.createArrayNode();
			audience.add(jsonNode.asText());
			objContent.replace("audience", audience);
			return Optional.of(objContent);
		}
		return Optional.empty();
	}
	
	static JsonNode getRoleAttributeSyntaxConfig()
	{
		ObjectNode main = Constants.MAPPER.createObjectNode();
		ArrayNode allow = main.putArray("allowed");
		for (String a : Arrays.asList("System Manager", "Contents Manager", "Privileged Inspector", "Inspector",
				"Regular User", "Policy documents manager", "Anonymous User"))
			allow.add(a);
		return main;
	}

	static String getEnRoleDescription()
	{
		return "Defines what operations are allowed for the bearer. The attribute of this type defines the access in the group where it is defined and in all subgroups. In subgroup it can be redefined to grant more access. Roles:\n "
				+ getEnRolesDescription();
	}

	private static String getEnRolesDescription()
	{
		StringBuilder ret = new StringBuilder(getOrgEnRoleDescription());
		ret.append(
				"<b>Policy documents manager</b> - Extends Regular User role with ability to manage policy documents\n");
		return ret.toString();
	}

	static String getOrgEnRoleDescription()
	{
		return "Defines what operations are allowed for the bearer. The attribute of this type defines the access in the group where it is defined and in all subgroups. In subgroup it can be redefined to grant more access. Roles:\n "
				+ getOrgEnRolesDescription();
	}

	private static String getOrgEnRolesDescription()
	{
		StringBuilder ret = new StringBuilder();
		ret.append("<b>System Manager</b> - System manager with all privileges.\n"
				+ "<b>Contents Manager</b> - Allows for performing all management operations related to groups, entities and attributes. Also allows for reading information about hidden attributes.\n"
				+ "<b>Privileged Inspector</b> - Allows for reading entities, groups and attributes, including the attributes visible locally only. No modifications are possible\n"
				+ "<b>Inspector</b> - Allows for reading entities, groups and attributes. No modifications are possible\n"
				+ "<b>Regular User</b> - Allows owners for reading of the basic system information, retrieval of information about themselves and also for changing self managed attributes, identities and passwords\n"
				+ "<b>Anonymous User</b> - Allows for minimal access to the system: owners can get basic system information and retrieve information about themselves\n");
		return ret.toString();
	}

}
