/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.to3_5;

import java.util.Arrays;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.types.I18nString;

class UpdateHelperTo13
{
	static void updateValuesJson(ArrayNode valuesArray)
	{
		for (int i = 0; i < valuesArray.size(); i++)
		{
			String jpegValue = valuesArray.get(i).asText();
			String updatedValue = "{\"type\":\"JPG\",\"value\":\"" + jpegValue + "\"}";
			valuesArray.remove(i);
			valuesArray.insert(i, updatedValue);
		}
	}

	static JsonNode getProjectRoleAttributeSyntaxConfig()
	{
		ObjectNode main = Constants.MAPPER.createObjectNode();
		ArrayNode allow = main.putArray("allowed");
		for (String a : Arrays.asList("manager", "projectsAdmin", "regular"))
			allow.add(a);
		return main;
	}

	static I18nString getProjectRoleDescription()
	{
		return new I18nString(
				"Controls authorization level in UpMan (Unity projects management). Must be set in the delegated group of the project. Roles:\n"
						+ getProjecRolesDescription());
	}

	private static String getProjecRolesDescription()
	{
		StringBuilder ret = new StringBuilder();
		ret.append("<br><b>").append("manager").append("</b> - ").append("Complete project management")
				.append("\n");
		ret.append("<br><b>").append("projectsAdmin").append("</b> - ").append(
				"Complete project management plus ability to create sub-projects (if root project settings allows for that)")
				.append("\n");
		ret.append("<br><b>").append("regular").append("</b> - ").append("No administration capabilitie")
				.append("\n");
		return ret.toString();
	}
}
