/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.to3_4;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.i18n.I18nStringJsonUtil;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.export.JsonDumpUpdate;

/**
 * 1. changes the legacy jpegImage to a properly implemented image
 * 2. changes the project management role attribute
 */
@Component
public class JsonDumpUpdateFromV11 implements JsonDumpUpdate
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, JsonDumpUpdateFromV11.class);

	@Autowired
	private ObjectMapper objectMapper;

	@Override
	public int getUpdatedVersion()
	{
		return 11;
	}

	@Override
	public InputStream update(InputStream is) throws IOException
	{
		ObjectNode root = (ObjectNode) objectMapper.readTree(is);

		JsonNode contents = root.get("contents");
		
		udpateAttributeTypes(contents.withArray("attributeTypes"));
		updateJpegAttributeValuesSyntax(contents.withArray("attributes"));
		dropAdminUIEndpoint(contents.withArray("endpointDefinition"));
		return new ByteArrayInputStream(objectMapper.writeValueAsBytes(root));

	}
	
	private void dropAdminUIEndpoint(ArrayNode endpointsArray)
	{
		for (int i=endpointsArray.size()-1; i>=0; i--)
		{
			JsonNode endpoint = endpointsArray.get(i);
			JsonNode parsed = endpoint.get("obj");
			if ("WebAdminUI".equals(parsed.get("typeId").asText()))
			{
				log.info("Dropping AdminUI endpoint {}", parsed.get("name"));
				endpointsArray.remove(i);
			}
		}
	}
	
	private void updateJpegAttributeValuesSyntax(JsonNode attributesArray)
	{
		for (JsonNode attrNode: attributesArray)
		{
			ObjectNode attrObj = (ObjectNode) attrNode;
			String syntax = attrObj.get("valueSyntax").asText();
			if (syntax.equals("jpegImage"))
			{
				log.info("Converting attribute {} (of {} in {}) to image syntax type", attrNode.get("name"),
						attrNode.get("entityId"), attrNode.get("groupPath"));
				attrObj.put("valueSyntax", "image");
				
				ArrayNode valuesArray = attrNode.withArray("values");
				UpdateHelperTo12.updateValuesJson(valuesArray);
			}
		}
	}

	private void udpateAttributeTypes(JsonNode attributeTypesArray)
	{
		for (JsonNode attrTypeNode: attributeTypesArray)
		{
			ObjectNode attrTypeObj = (ObjectNode) attrTypeNode;
			String syntax = attrTypeObj.get("syntaxId").asText();
			String name = attrTypeObj.get("name").asText();
			
			if (syntax.equals("jpegImage"))
			{
				log.info("Converting attribute type {} to use image syntax type", 
						attrTypeObj.get("name").asText());
				attrTypeObj.put("syntaxId", "image");
			}
			
			if (name.equals("sys:ProjectManagementRole"))
			{
				log.info("Updating attribute type {} adding new value projectsAdmin", 
						attrTypeObj.get("name").asText());
				attrTypeObj.set("syntaxState", UpdateHelperTo12.getProjectRoleAttributeSyntaxConfig());
				
				if (I18nStringJsonUtil.fromJson(attrTypeObj.get("i18nDescription")).isEmpty())
				{
					attrTypeObj.set("i18nDescription", I18nStringJsonUtil
							.toJson(UpdateHelperTo12.getProjectRoleDescription()));
				}
			}
		}
	}
}