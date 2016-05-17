/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.export;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.base.registries.AttributeSyntaxFactoriesRegistry;
import pl.edu.icm.unity.base.registries.IdentityTypesRegistry;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;
import pl.edu.icm.unity.types.basic.IdentityTypeDefinition;
import pl.edu.icm.unity.types.basic.VerifiableEmail;

/**
 * Updates a JSON dump before it is actually imported.
 * Changes are performed in JSON contents, input stream is reset after the changes are performed.
 * @author K. Benedyczak
 */
@Component
public class UpdateFrom1_9_x implements Update
{
	@Autowired
	private ObjectMapper objectMapper;
	
	@Autowired 
	private IdentityTypesRegistry idTypesRegistry;
	
	@Autowired 
	private AttributeSyntaxFactoriesRegistry attributeSyntaxFactoriesRegistry;
	
	@Override
	public void update(InputStream is) throws IOException
	{
		JsonFactory jsonF = new JsonFactory(objectMapper);
		JsonParser jp = jsonF.createParser(is);
		JsonUtils.nextExpect(jp, JsonToken.START_OBJECT);
		JsonUtils.nextExpect(jp, "contents");
		
		
		Map<Long, ObjectNode> attributeTypesById = new HashMap<>();
		Map<String, ObjectNode> attributeTypesByName = new HashMap<>();
		ArrayNode attrTypes = JsonUtils.deserialize2Array(jp, "attributeTypes");
		for (JsonNode node: attrTypes)
		{
			long id = node.get("id").asLong();
			String name = node.get("name").asText();
			attributeTypesById.put(id, (ObjectNode) node);
			attributeTypesByName.put(name, (ObjectNode) node);
		}
		
		ArrayNode idTypes = JsonUtils.deserialize2Array(jp, "identityTypes");
		for (JsonNode node: idTypes)
		{
			ObjectNode oNode = (ObjectNode) node;
			oNode.put("identityTypeProvider", node.get("name").asText());
		}

		JsonUtils.nextExpect(jp, "entities");
		jp.skipChildren();

		ArrayNode ids = JsonUtils.deserialize2Array(jp, "identities");
		for (JsonNode node: ids)
			setIdentityComparableValue((ObjectNode) node);

		ArrayNode groups = JsonUtils.deserialize2Array(jp, "groups");
		updateGroups(groups, attributeTypesById);
		
//		JsonUtils.nextExpect(jp, "groupMembers");
//		groupMembersIE.deserialize(sql, jp);

		ArrayNode attributes = JsonUtils.deserialize2Array(jp, "attributes");
		for (JsonNode node: attributes)
			updateStoredAttribute((ObjectNode) node, attributeTypesByName);

//		JsonUtils.nextExpect(jp, "genericObjects");
//		genericsIE.deserialize(sql, jp);
	}
	
	private void setIdentityComparableValue(ObjectNode src)
	{
		String type = src.get("typeId").asText();
		IdentityTypeDefinition idTypeDef = idTypesRegistry.getByName(type);
		String comparable;
		try
		{
			comparable = idTypeDef.getComparableValue(src.get("value").asText(), 
					src.get("realm").asText(null),
					src.get("target").asText(null));
		} catch (IllegalIdentityValueException e)
		{
			throw new InternalException("Can't deserialize identity: invalid value [" + 
					src.get("value") +"]", e);
		}
		src.put("comparableValue", comparable);
	}

	private void updateGroups(ArrayNode src, Map<Long, ObjectNode> attributeTypesById)
	{
		Map<Long, String> legacyGroupIds = new HashMap<>();
		for (JsonNode node: src)
		{
			String groupPath = node.get("groupPath").asText();
			long id = node.get("id").asLong();
			legacyGroupIds.put(id, groupPath);
		}
	
		for (JsonNode node: src)
			updateFromPre2Single((ObjectNode) node, legacyGroupIds, attributeTypesById);
	}
	
	
	private void updateFromPre2Single(ObjectNode src, Map<Long, String> legacyGroupIds, 
			Map<Long, ObjectNode> attributeTypesById)
	{
		String groupPath = src.get("groupPath").asText();
		src.put("path", groupPath);
		long id = src.get("id").asLong();
		legacyGroupIds.put(id, groupPath);
		
		ArrayNode oldStatements = (ArrayNode) src.get("attributeStatements");
		if (oldStatements == null)
			return;
		
		for (JsonNode statementO: oldStatements)
		{
			ObjectNode statement = (ObjectNode) statementO;
			if (statement.has("extraGroup"))
			{
				long extraGroupId = statement.get("extraGroup").asLong();
				statement.put("extraGroupName", legacyGroupIds.get(extraGroupId));
			}
			if (statement.has("fixedAttribute-attributeId"))
			{
				long attrId = statement.get("fixedAttribute-attributeId").asLong();
				long groupId = statement.get("fixedAttribute-attributeGroupId").asLong();
				String values = statement.get("fixedAttribute-attributeValues").asText();
				
				String group = legacyGroupIds.get(groupId);
				ObjectNode atDef = attributeTypesById.get(attrId);
				String attributeName = atDef.get("name").asText();
				String attributeSyntax = atDef.get("valueSyntaxId").asText();

				ObjectNode target = objectMapper.createObjectNode();
				toNewAttribute(values, target, attributeName, group, attributeSyntax);
				statement.set("fixedAttribute", target);
			}
		}
	}
	
	private void updateStoredAttribute(ObjectNode src, Map<String, ObjectNode> attributeTypesByName)
	{
		String attr = src.get("attributeName").asText();
		String group = src.get("groupPath").asText();
		String values = src.get("values").asText();
		ObjectNode atDef = attributeTypesByName.get(attr);
		String attributeSyntax = atDef.get("valueSyntaxId").asText();

		toNewAttribute(values, src, attr, group, attributeSyntax);
		src.put("entity", src.get("entity").asLong());
	}
	
	@SuppressWarnings("unchecked")
	private void toNewAttribute(String oldValues, ObjectNode target, String attributeName, String group,
			String valueSyntax)
	{
		target.put("name", attributeName);
		target.put("groupPath", group);
		target.put("valueSyntax", valueSyntax);
		
		ObjectNode old = JsonUtil.parse(oldValues);
		if (old.has("creationTs"))
			target.put("creationTs", old.get("creationTs").asLong());
		if (old.has("updateTs"))
			target.put("updateTs", old.get("updateTs").asLong());
		if (old.has("translationProfile"))
			target.put("translationProfile", old.get("translationProfile").asText());
		if (old.has("remoteIdp"))
			target.put("remoteIdp", old.get("remoteIdps").asText());
		target.put("direct", true);
		
		
		ArrayNode oldValuesA = old.withArray("values");
		ArrayNode newValuesA = target.withArray("values");
		@SuppressWarnings("rawtypes")
		AttributeValueSyntax syntax = attributeSyntaxFactoriesRegistry.
				getByName(valueSyntax).createInstance();
		try
		{
			for (JsonNode node: oldValuesA)
			{
				Object read = convertLegacyValue(node.binaryValue(), valueSyntax);
				newValuesA.add(syntax.convertToString(read));
			}
		} catch (Exception e)
		{
			throw new InternalException("Can't perform JSON deserialization", e);
		}
	}

	private Object convertLegacyValue(byte[] binaryValue, String valueSyntax) throws IOException
	{
		switch (valueSyntax)
		{
		case "string":
		case "enum":
			return new String(binaryValue, StandardCharsets.UTF_8);
		case "floatingPoint":
			ByteBuffer bb = ByteBuffer.wrap(binaryValue);
			return bb.getDouble();
		case "integer":
			bb = ByteBuffer.wrap(binaryValue);
			return bb.getLong();
		case "verifiableEmail":
			JsonNode jsonN = Constants.MAPPER.readTree(new String(binaryValue, StandardCharsets.UTF_8));
			return new VerifiableEmail(jsonN);
		case "jpegImage":
			ByteArrayInputStream bis = new ByteArrayInputStream(binaryValue);
			return ImageIO.read(bis);
		default:
			throw new IllegalStateException("Unknown attribute value type, can't be converted: " 
					+ valueSyntax);
		}
	}
}
