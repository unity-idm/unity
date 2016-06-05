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
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import eu.emi.security.authn.x509.impl.X500NameUtils;
import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.base.utils.Escaper;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.store.objstore.ac.AttributeClassHandler;
import pl.edu.icm.unity.store.objstore.authn.AuthenticatorInstanceHandler;
import pl.edu.icm.unity.store.objstore.bulk.ProcessingRuleHandler;
import pl.edu.icm.unity.store.objstore.confirmation.ConfirmationConfigurationHandler;
import pl.edu.icm.unity.store.objstore.cred.CredentialHandler;
import pl.edu.icm.unity.store.objstore.credreq.CredentialRequirementHandler;
import pl.edu.icm.unity.store.objstore.endpoint.EndpointHandler;
import pl.edu.icm.unity.store.objstore.msgtemplate.MessageTemplateHandler;
import pl.edu.icm.unity.store.objstore.notify.NotificationChannelHandler;
import pl.edu.icm.unity.store.objstore.realm.RealmHandler;
import pl.edu.icm.unity.store.objstore.reg.eform.EnquiryFormHandler;
import pl.edu.icm.unity.store.objstore.reg.eresp.EnquiryResponseHandler;
import pl.edu.icm.unity.store.objstore.reg.form.RegistrationFormHandler;
import pl.edu.icm.unity.store.objstore.reg.invite.InvitationHandler;
import pl.edu.icm.unity.store.objstore.reg.req.RegistrationRequestHandler;
import pl.edu.icm.unity.store.objstore.tprofile.InputTranslationProfileHandler;
import pl.edu.icm.unity.store.objstore.tprofile.OutputTranslationProfileHandler;
import pl.edu.icm.unity.types.basic.VerifiableEmail;

/**
 * Updates a JSON dump before it is actually imported.
 * Changes are performed in JSON contents, input stream is reset after the changes are performed.
 * 
 * TODO - InvitationWithCode - 2xtime changed from second to millisecond, apply *1000
 * 
 * @author K. Benedyczak
 */
@Component
public class UpdateFrom1_9_x implements Update
{
	@Autowired
	private ObjectMapper objectMapper;
	
	@Override
	public InputStream update(InputStream is) throws IOException
	{
		ObjectNode root = (ObjectNode) objectMapper.readTree(is);
		ObjectNode contents = (ObjectNode) root.get("contents");
		
		Map<Long, ObjectNode> attributeTypesById = new HashMap<>();
		Map<String, ObjectNode> attributeTypesByName = new HashMap<>();
		
		updateAttributeTypes(attributeTypesById, attributeTypesByName, contents);
		
		updateIdentityTypes(contents);
		
		updateIdentitites(contents);
		
		updateEntities(contents);

		updateGroups(contents, attributeTypesById);

		updateMembers(contents);
		
		updateAttributes(contents, attributeTypesByName);

		updateGenerics(contents);
		
		return new ByteArrayInputStream(objectMapper.writeValueAsBytes(root));
	}

	private void updateGenerics(ObjectNode contents)
	{
		ArrayNode generics = (ArrayNode) contents.get("genericObjects");
		if (generics == null)
			return;
		ObjectNode newGenerics = contents;
		
		Map<String, List<ObjectNode>> genericsByType = sortGenerics(generics);
		
		convertGenericType(AttributeClassHandler.ATTRIBUTE_CLASS_OBJECT_TYPE, 
				genericsByType, newGenerics);
		convertGenericType(AuthenticatorInstanceHandler.AUTHENTICATOR_OBJECT_TYPE, 
				genericsByType, newGenerics);
		convertGenericType(ConfirmationConfigurationHandler.CONFIRMATION_CONFIGURATION_OBJECT_TYPE, 
				genericsByType, newGenerics);
		convertGenericType(CredentialHandler.CREDENTIAL_OBJECT_TYPE, 
				genericsByType, newGenerics);
		convertGenericType(CredentialRequirementHandler.CREDENTIAL_REQ_OBJECT_TYPE, 
				genericsByType, newGenerics);
		convertGenericType(MessageTemplateHandler.MESSAGE_TEMPLATE_OBJECT_TYPE, 
				genericsByType, newGenerics);
		convertGenericType(NotificationChannelHandler.NOTIFICATION_CHANNEL_ID, 
				genericsByType, newGenerics);
		convertGenericType(RealmHandler.REALM_OBJECT_TYPE, 
				genericsByType, newGenerics);
		convertGenericType(InputTranslationProfileHandler.TRANSLATION_PROFILE_OBJECT_TYPE, //FIXME
				genericsByType, newGenerics);
		convertGenericType(OutputTranslationProfileHandler.TRANSLATION_PROFILE_OBJECT_TYPE, //FIXME
				genericsByType, newGenerics);
		convertGenericType(ProcessingRuleHandler.PROCESSING_RULE_OBJECT_TYPE, 
				genericsByType, newGenerics);
		convertGenericType(EndpointHandler.ENDPOINT_OBJECT_TYPE, 
				genericsByType, newGenerics);
		convertGenericType(RegistrationFormHandler.REGISTRATION_FORM_OBJECT_TYPE, 
				genericsByType, newGenerics);
		convertGenericType(EnquiryFormHandler.ENQUIRY_FORM_OBJECT_TYPE, 
				genericsByType, newGenerics);
		convertGenericType(RegistrationRequestHandler.REGISTRATION_REQUEST_OBJECT_TYPE, 
				genericsByType, newGenerics);
		convertGenericType(EnquiryResponseHandler.ENQUIRY_RESPONSE_OBJECT_TYPE, 
				genericsByType, newGenerics);
		convertGenericType(InvitationHandler.INVITATION_OBJECT_TYPE, 
				genericsByType, newGenerics);
		
		contents.remove("genericObjects");
	}
	
	private void convertGenericType(String type, Map<String, List<ObjectNode>> genericsByType, 
			ObjectNode newGenerics)
	{
		ArrayNode typeArray = (ArrayNode) newGenerics.withArray(type);
		List<ObjectNode> list = genericsByType.get(type);
		if (list == null)
			return;
		
		for (ObjectNode obj: list)
		{
			typeArray.add(obj.get("contents"));
		}
	}

	private Map<String, List<ObjectNode>> sortGenerics(ArrayNode generics)
	{
		Map<String, List<ObjectNode>> genericsByType = new HashMap<>();
		
		for (JsonNode node: generics)
		{
			String type = node.get("type").asText();
			List<ObjectNode> list = genericsByType.get(type);
			if (list == null)
			{
				list = new ArrayList<>();
				genericsByType.put(type, list);
			}
			list.add((ObjectNode) node);
		}
		return genericsByType;
	}

	private void updateAttributeTypes(Map<Long, ObjectNode> attributeTypesById, 
			Map<String, ObjectNode> attributeTypesByName, ObjectNode contents)
	{
		ArrayNode attrTypes = (ArrayNode) contents.get("attributeTypes");
		if (attrTypes == null)
			return;
		for (JsonNode node: attrTypes)
		{
			long id = node.get("id").asLong();
			String name = node.get("name").asText();
			attributeTypesById.put(id, (ObjectNode) node);
			attributeTypesByName.put(name, (ObjectNode) node);
			
			ObjectNode nodeO = (ObjectNode) node; 
			nodeO.set("syntaxId", node.get("valueSyntaxId"));
			nodeO.setAll((ObjectNode)node.get("contents"));
		}
	}
	
	private void updateIdentityTypes(ObjectNode contents)
	{
		ArrayNode idTypes = (ArrayNode) contents.get("identityTypes"); 
		if (idTypes == null)
			return;
		for (JsonNode node: idTypes)
		{
			ObjectNode oNode = (ObjectNode) node;
			oNode.put("identityTypeProvider", node.get("name").asText());
			oNode.setAll((ObjectNode)node.get("contents"));
		}
	}

	private void updateEntities(ObjectNode contents)
	{
		ArrayNode entities = (ArrayNode) contents.get("entities"); 
		if (entities == null)
			return;
		for (JsonNode node: entities)
		{
			ObjectNode oNode = (ObjectNode) node;
			oNode.setAll((ObjectNode)node.get("contents"));
			oNode.set("entityId", node.get("id"));
		}
	}

	
	private void updateIdentitites(ObjectNode contents) throws IOException
	{
		ArrayNode ids = (ArrayNode) contents.get("identities");
		if (ids == null)
			return;
		for (JsonNode node: ids)
			updateIdentity((ObjectNode) node);
	}
	
	private void updateIdentity(ObjectNode src) throws IOException
	{
		String type = src.get("typeName").asText();
		ObjectNode contents = (ObjectNode) src.get("contents");
		String comparable = getComparableIdentityValue(type, 
				contents.get("value").asText(),
				JsonUtil.getWithDef(contents, "realm", null),
				JsonUtil.getWithDef(contents, "target", null));
		src.setAll(contents);
		src.put("typeId", type);
		src.put("comparableValue", comparable);
		if (contents.has("confirmationInfo"))
		{
			JsonNode ci = objectMapper.readTree(contents.get("confirmationInfo").asText());
			src.set("confirmationInfo", ci);
		}
		if (!contents.has("creationTs"))
			src.put("creationTs", 0);
		if (!contents.has("updateTs"))
			src.put("updateTs", 0);
	}

	private String getComparableIdentityValue(String type, String value, String realm, String target)
	{
		switch (type)
		{
		case "identifier":
		case "persistent":
		case "userName":
			return value;
			
		case "email":
			return new VerifiableEmail(value).getComparableValue();
			
		case "targetedPersistent":
			return Escaper.encode(realm, target, value);
			
		case "transient":
			return null;
			
		case "x500Name":
			return X500NameUtils.getComparableForm(value);
			
		default:
			throw new IllegalStateException("Unknown identity type, can't be converted: " + type);
		}
	}

	private void updateGroups(ObjectNode contents, Map<Long, ObjectNode> attributeTypesById)
	{
		ArrayNode src = (ArrayNode) contents.get("groups");
		Map<Long, String> legacyGroupIds = new HashMap<>();
		if (src == null)
			return;
		for (JsonNode node: src)
		{
			String groupPath = node.get("groupPath").asText();
			long id = node.get("id").asLong();
			legacyGroupIds.put(id, groupPath);
		}
	
		Iterator<JsonNode> iterator = src.iterator();
		while (iterator.hasNext())
		{
			JsonNode node = iterator.next();
			if (node.get("name").asText().equals("ROOT"))
			{
				iterator.remove();
				continue;
			}
			updateGroup((ObjectNode) node, legacyGroupIds, attributeTypesById);
		}
	}
	
	
	private void updateGroup(ObjectNode src, Map<Long, String> legacyGroupIds, 
			Map<Long, ObjectNode> attributeTypesById)
	{
		String groupPath = src.get("groupPath").asText();
		src.put("path", groupPath);
		long id = src.get("id").asLong();
		legacyGroupIds.put(id, groupPath);
		
		if (!src.has("attributesClasses"))
			src.putArray("attributesClasses");
		
		ArrayNode oldStatements = (ArrayNode) src.get("attributeStatements");
		if (oldStatements == null)
		{
			src.putArray("attributeStatements");
			return;
		}
		
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

	private void updateMembers(ObjectNode contents) throws IOException
	{
		ArrayNode members = (ArrayNode) contents.get("groupMembers");
		if (members == null)
			return;
		ArrayNode newMembers = objectMapper.createArrayNode();
		for (JsonNode node: members)
			updateGroupMembers((ObjectNode) node, newMembers);
		contents.replace("groupMembers", newMembers);
	}

	private void updateGroupMembers(ObjectNode src, ArrayNode newMembers) throws IOException
	{
		String group = src.get("groupPath").asText();
		
		ArrayNode jsonNode = (ArrayNode) src.get("members");
		for (JsonNode gMember: jsonNode)
		{
			ObjectNode newMember = newMembers.addObject();
			newMember.put("group", group);
			newMember.put("entityId", gMember.get("entity").asLong());
			if (gMember.has("contents"))
			{
				String contents64 = gMember.get("contents").asText();
				String contents = new String(Base64.getDecoder().decode(contents64), 
						StandardCharsets.UTF_8);
				newMember.setAll((ObjectNode)objectMapper.readTree(contents));
			}
		}
	}
	
	
	private void updateAttributes(ObjectNode contents, Map<String, ObjectNode> attributeTypesByName)
	{
		ArrayNode attributes = (ArrayNode) contents.get("attributes");
		if (attributes == null)
			return;
		for (JsonNode node: attributes)
			updateStoredAttribute((ObjectNode) node, attributeTypesByName);
	}
	
	private void updateStoredAttribute(ObjectNode src, Map<String, ObjectNode> attributeTypesByName)
	{
		String attr = src.get("attributeName").asText();
		String group = src.get("groupPath").asText();
		String values = src.remove("values").asText();
		ObjectNode atDef = attributeTypesByName.get(attr);
		String attributeSyntax = atDef.get("valueSyntaxId").asText();

		toNewAttribute(values, src, attr, group, attributeSyntax);
		src.put("entityId", src.get("entity").asLong());
	}
	
	private void toNewAttribute(String oldValues, ObjectNode target, String attributeName, String group,
			String valueSyntax)
	{
		target.put("name", attributeName);
		target.put("groupPath", group);
		target.put("valueSyntax", valueSyntax);
		
		byte[] base64decoded = Base64.getDecoder().decode(oldValues);
		ObjectNode old = JsonUtil.parse(new String(base64decoded, StandardCharsets.UTF_8));
		if (old.has("creationTs"))
			target.put("creationTs", old.get("creationTs").asLong());
		if (old.has("updateTs"))
			target.put("updateTs", old.get("updateTs").asLong());
		if (old.has("translationProfile"))
			target.put("translationProfile", old.get("translationProfile").asText());
		if (old.has("remoteIdp"))
			target.put("remoteIdp", old.get("remoteIdp").asText());
		target.put("direct", true);
		
		
		ArrayNode oldValuesA = old.withArray("values");
		ArrayNode newValuesA = target.withArray("values");
		try
		{
			for (JsonNode node: oldValuesA)
			{
				String converted = convertLegacyAttributeValue(node.binaryValue(), valueSyntax);
				newValuesA.add(converted);
			}
		} catch (Exception e)
		{
			throw new InternalException("Can't perform JSON deserialization", e);
		}
	}

	private String convertLegacyAttributeValue(byte[] binaryValue, String valueSyntax) throws IOException
	{
		switch (valueSyntax)
		{
		case "string":
		case "enumeration":
			return new String(binaryValue, StandardCharsets.UTF_8);
		case "floatingPoint":
			ByteBuffer bb = ByteBuffer.wrap(binaryValue);
			return Double.toString(bb.getDouble());
		case "integer":
			bb = ByteBuffer.wrap(binaryValue);
			return Long.toString(bb.getLong());
		case "verifiableEmail":
			JsonNode jsonN = Constants.MAPPER.readTree(new String(binaryValue, StandardCharsets.UTF_8));
			return JsonUtil.serialize(jsonN);
		case "jpegImage":
			return Base64.getEncoder().encodeToString(binaryValue);
		default:
			throw new IllegalStateException("Unknown attribute value type, can't be converted: " 
					+ valueSyntax);
		}
	}
}
