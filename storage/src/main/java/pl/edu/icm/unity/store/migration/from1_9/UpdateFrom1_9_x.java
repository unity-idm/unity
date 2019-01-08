/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.migration.from1_9;

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

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import eu.emi.security.authn.x509.impl.X500NameUtils;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.base.utils.Escaper;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.store.export.Update;
import pl.edu.icm.unity.store.objstore.ac.AttributeClassHandler;
import pl.edu.icm.unity.store.objstore.authn.AuthenticatorConfigurationHandler;
import pl.edu.icm.unity.store.objstore.bulk.ProcessingRuleHandler;
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
 * @author K. Benedyczak
 */
@Component
public class UpdateFrom1_9_x implements Update
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, UpdateFrom1_9_x.class);
	@Autowired
	private ObjectMapper objectMapper;
	
	@Override
	public InputStream update(InputStream is) throws IOException
	{
		ObjectNode root = (ObjectNode) objectMapper.readTree(is);
		ObjectNode contents = (ObjectNode) root.get("contents");
		
		UpdateContext ctx = new UpdateContext();
		
		updateAttributeTypes(ctx, contents);
		
		updateIdentityTypes(contents);
		
		updateIdentitites(contents);
		
		updateEntities(contents);

		updateGroups(contents, ctx);

		updateMembers(contents);
		
		updateAttributes(contents, ctx);

		updateGenerics(contents, ctx);
		
		return new ByteArrayInputStream(objectMapper.writeValueAsBytes(root));
	}

	private void updateGenerics(ObjectNode contents, UpdateContext ctx) throws IOException
	{
		ArrayNode generics = (ArrayNode) contents.get("genericObjects");
		if (generics == null)
			return;
		ObjectNode newGenerics = contents;
		
		Map<String, List<ObjectNode>> genericsByType = sortGenerics(generics);
		
		convertGenericType(AttributeClassHandler.ATTRIBUTE_CLASS_OBJECT_TYPE, 
				genericsByType, newGenerics, ctx);
		convertGenericType(AuthenticatorConfigurationHandler.AUTHENTICATOR_OBJECT_TYPE, 
				genericsByType, newGenerics, ctx);
		convertGenericType("confirmationConfiguration", 
				genericsByType, newGenerics, ctx);
		convertGenericType(CredentialHandler.CREDENTIAL_OBJECT_TYPE, 
				genericsByType, newGenerics, ctx);
		convertGenericType(CredentialRequirementHandler.CREDENTIAL_REQ_OBJECT_TYPE, 
				genericsByType, newGenerics, ctx);
		convertGenericType(MessageTemplateHandler.MESSAGE_TEMPLATE_OBJECT_TYPE, 
				genericsByType, newGenerics, ctx);
		convertGenericType(NotificationChannelHandler.NOTIFICATION_CHANNEL_ID, 
				genericsByType, newGenerics, ctx);
		convertGenericType(RealmHandler.REALM_OBJECT_TYPE, 
				genericsByType, newGenerics, ctx);
		convertTranslationProfiles(genericsByType, newGenerics, ctx);
		convertGenericType(ProcessingRuleHandler.PROCESSING_RULE_OBJECT_TYPE, 
				genericsByType, newGenerics, ctx);
		convertGenericType(EndpointHandler.ENDPOINT_OBJECT_TYPE, 
				genericsByType, newGenerics, ctx);
		convertGenericType(RegistrationFormHandler.REGISTRATION_FORM_OBJECT_TYPE, 
				genericsByType, newGenerics, ctx);
		convertGenericType(EnquiryFormHandler.ENQUIRY_FORM_OBJECT_TYPE, 
				genericsByType, newGenerics, ctx);
		convertGenericType(RegistrationRequestHandler.REGISTRATION_REQUEST_OBJECT_TYPE, 
				genericsByType, newGenerics, ctx);
		convertGenericType(EnquiryResponseHandler.ENQUIRY_RESPONSE_OBJECT_TYPE, 
				genericsByType, newGenerics, ctx);
		convertGenericType(InvitationHandler.INVITATION_OBJECT_TYPE, 
				genericsByType, newGenerics, ctx);
		
		contents.remove("genericObjects");
	}
	
	private void convertGenericType(String type, Map<String, List<ObjectNode>> genericsByType, 
			ObjectNode newGenerics, UpdateContext ctx) throws IOException
	{
		ArrayNode typeArray = (ArrayNode) newGenerics.withArray(type);
		List<ObjectNode> list = genericsByType.get(type);
		if (list == null)
			return;
		
		for (ObjectNode obj: list)
		{
			ObjectNode newGeneric = objectMapper.createObjectNode();
			typeArray.add(newGeneric);
			ObjectNode content = (ObjectNode) obj.get("contents");
			newGeneric.set("_updateTS", obj.get("lastUpdate"));
			newGeneric.set("obj", content);
			
			switch (type)
			{
			case RealmHandler.REALM_OBJECT_TYPE:
			case NotificationChannelHandler.NOTIFICATION_CHANNEL_ID:
				content.put("name", obj.get("name").asText());
				break;
			case EndpointHandler.ENDPOINT_OBJECT_TYPE:
				updateEndpoint(obj, content);
				break;
			case RegistrationRequestHandler.REGISTRATION_REQUEST_OBJECT_TYPE:
			case EnquiryResponseHandler.ENQUIRY_RESPONSE_OBJECT_TYPE:
				updateRegistrationOrEnquiryRequest(content, ctx);
				break;
			case InvitationHandler.INVITATION_OBJECT_TYPE:
				updateInvitation(content, ctx);
				break;
			case CredentialHandler.CREDENTIAL_OBJECT_TYPE:
				updateCredential(content, ctx);
				break;
			case AuthenticatorConfigurationHandler.AUTHENTICATOR_OBJECT_TYPE:
				updateAuthenticator(content, ctx);
				break;
			case EnquiryFormHandler.ENQUIRY_FORM_OBJECT_TYPE:
			case RegistrationFormHandler.REGISTRATION_FORM_OBJECT_TYPE:
				updateRegistrationOrEnquiryForm(content, ctx);
			}
		}
	}

	//remove 'visibility' action param
	private void updateRegistrationOrEnquiryForm(ObjectNode content, UpdateContext ctx)
	{
		if (content.has("TranslationProfile"))
		{
			JsonNode tp = content.get("TranslationProfile");
			updateTranslationProfile((ObjectNode) tp);
		}
	}

	//remove 'visibility' action param
	private void updateTranslationProfile(ObjectNode tp)
	{
		ArrayNode rules = (ArrayNode) tp.get("rules");
		for (JsonNode rule: rules)
		{
			JsonNode action = rule.get("action");
			String name = action.get("name").asText();
			if (name.equals("addAttribute") || name.equals("mapAttribute"))
			{
				ArrayNode params = (ArrayNode) action.get("parameters");
				params.remove(3);
			}
		}
	}
	
	private void convertTranslationProfiles(Map<String, List<ObjectNode>> genericsByType, 
			ObjectNode newGenerics, UpdateContext ctx) throws IOException
	{
		ArrayNode inputArray = (ArrayNode) newGenerics.withArray(
				InputTranslationProfileHandler.TRANSLATION_PROFILE_OBJECT_TYPE);
		ArrayNode outputArray = (ArrayNode) newGenerics.withArray(
				OutputTranslationProfileHandler.TRANSLATION_PROFILE_OBJECT_TYPE);
		
		List<ObjectNode> list = genericsByType.get("translationProfile");
		if (list == null)
			return;
		
		for (ObjectNode obj: list)
		{
			ObjectNode newGeneric = objectMapper.createObjectNode();
			ObjectNode content = (ObjectNode) obj.get("contents");
			newGeneric.set("_updateTS", obj.get("lastUpdate"));
			newGeneric.set("obj", content);

			if ((obj.get("subType") != null && obj.get("subType").asText().equals("OUTPUT")) ||
					(content.get("type") != null && content.get("type").asText().equals("OUTPUT")))
				outputArray.add(newGeneric);
			else
				inputArray.add(updateInputProfileContent(newGeneric));
		}
	}

	private ObjectNode updateInputProfileContent(ObjectNode content)
	{
		if (!content.has("obj"))
			return content;
		JsonNode internal = content.get("obj");
		if (!internal.has("rules"))
			return content;
		ArrayNode rules = (ArrayNode) internal.get("rules");
		for (JsonNode rule: rules)
		{
			JsonNode action = rule.get("action");
			String actionName = action.get("name").asText();
			ArrayNode params = (ArrayNode) action.get("parameters");
			switch (actionName)
			{
			case "mapAttribute":
				params.remove(3);
				break;
			case "multiMapAttribute":
				params.remove(1);
				break;
			}
		}
		return content;
	}
	
	
	private void updateInvitation(ObjectNode target, UpdateContext ctx)
	{
		long expiry = target.get("expiration").asLong();
		target.put("expiration", expiry*1000);
		if (target.has("lastSentTime"))
		{
			long lastSent = target.get("lastSentTime").asLong();
			target.put("lastSentTime", lastSent*1000);
		}
		ObjectNode attributesOld = (ObjectNode) target.get("attributes");
		attributesOld.fields().forEachRemaining(field ->
		{
			ObjectNode el = (ObjectNode) field.getValue();
			ObjectNode oldEntry = (ObjectNode) el.get("attribute");
			try
			{
				updateParamAttribute(oldEntry, ctx);
				el.set("entry", oldEntry);
			} catch (Exception e)
			{
				log.warn("Can't update invitation perfilled attribute, skipping it", e);
				el.set("entry", null);
			}
		});
	}

	private void updateRegistrationOrEnquiryRequest(ObjectNode target, UpdateContext ctx) 
			throws IOException
	{
		ArrayNode attributes = (ArrayNode) target.get("Attributes");
		for (JsonNode attributeN: attributes)
		{
			if (attributeN.isNull())
				continue;
			ObjectNode attribute = (ObjectNode) attributeN;
			attribute.setAll((ObjectNode)attribute.get("attribute"));
			updateParamAttribute(attribute, ctx);
		}
	}
	
	private void updateParamAttribute(ObjectNode attribute, UpdateContext ctx) throws IOException
	{
		attribute.set("remoteIdp", attribute.get("externalIdp"));
		ObjectNode atDef = ctx.attributeTypesByName.get(attribute.get("name").asText());
		String attributeSyntax = atDef.get("valueSyntaxId").asText();
		attribute.put("valueSyntax", attributeSyntax);
		ArrayNode valuesOld = (ArrayNode) attribute.remove("values");
		ArrayNode valuesNew = objectMapper.createArrayNode();
		for (JsonNode valueOld: valuesOld)
			valuesNew.add(convertLegacyAttributeValue(valueOld.binaryValue(), attributeSyntax));
		attribute.set("values", valuesNew);
	}

	private void updateEndpoint(ObjectNode old, ObjectNode target)
	{
		ObjectNode oldContent = (ObjectNode) old.get("contents"); 
		ObjectNode description = (ObjectNode) oldContent.get("description");
		target.put("name", description.get("id").asText());
		target.put("typeId", description.get("typeName").asText());
		target.put("contextAddress", description.get("contextAddress").asText());
		ObjectNode targetConfig = target.with("configuration");
		targetConfig.set("displayedName", description.get("displayedName"));
		targetConfig.set("description", description.get("description"));
		targetConfig.set("realm", description.get("realmName"));
		targetConfig.set("authenticationOptions", description.get("authenticationOptions"));
		targetConfig.set("configuration", oldContent.get("state"));
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

	private void updateAttributeTypes(UpdateContext ctx, ObjectNode contents)
	{
		ArrayNode attrTypes = (ArrayNode) contents.get("attributeTypes");
		if (attrTypes == null)
			return;
		for (JsonNode node: attrTypes)
		{
			long id = node.get("id").asLong();
			String name = node.get("name").asText();
			ctx.attributeTypesById.put(id, (ObjectNode) node);
			ctx.attributeTypesByName.put(name, (ObjectNode) node);
			
			ObjectNode nodeO = (ObjectNode) node; 
			nodeO.set("syntaxId", node.get("valueSyntaxId"));
			nodeO.setAll((ObjectNode)node.get("contents"));
			JsonNode oldSyntax = nodeO.remove("syntaxState");
			ObjectNode syntaxCfg = JsonUtil.parse(oldSyntax.asText());
			nodeO.set("syntaxState", syntaxCfg);
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
			if (!oNode.has("selfModificable"))
				oNode.put("selfModificable", false);
			
			if (!oNode.has("minInstances"))
				oNode.put("minInstances", 0);
			if (!oNode.has("minVerifiedInstances"))
				oNode.put("minVerifiedInstances", 0);
			if (!oNode.has("maxInstances"))
				oNode.put("maxInstances", Integer.MAX_VALUE);
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


	private void updateCredential(ObjectNode content, UpdateContext ctx) throws IOException
	{
		if (!content.has("jsonConfiguration"))
			return;
		String config = content.remove("jsonConfiguration").asText();
		
		if (!config.isEmpty())
		{
			ObjectNode configJson = (ObjectNode) objectMapper.readTree(config);
			configJson.remove("rehashNumber");
			config = objectMapper.writeValueAsString(configJson);
		}
		
		content.put("configuration", config);
	}


	private void updateAuthenticator(ObjectNode content, UpdateContext ctx) throws IOException
	{
		if (content.has("retrievalJsonConfiguration"))
		{
			JsonNode removed = content.remove("retrievalJsonConfiguration");
			if (!removed.isNull())
				content.put("retrievalConfiguration", removed.asText());
		}
		if (content.has("verificatorJsonConfiguration"))
		{
			JsonNode removed = content.remove("verificatorJsonConfiguration");
			if (!removed.isNull())
				content.put("verificatorConfiguration", removed.asText());
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

	private void updateGroups(ObjectNode contents, UpdateContext ctx)
	{
		ArrayNode src = (ArrayNode) contents.get("groups");
		
		if (src == null)
			return;
		for (JsonNode node: src)
		{
			String groupPath = node.get("groupPath").asText();
			long id = node.get("id").asLong();
			ctx.legacyGroupIds.put(id, groupPath);
		}
	
		Iterator<JsonNode> iterator = src.iterator();
		while (iterator.hasNext())
		{
			JsonNode node = iterator.next();
			updateGroup((ObjectNode) node, ctx);
		}
	}
	
	
	private void updateGroup(ObjectNode src, UpdateContext ctx)
	{
		String groupPath = src.get("groupPath").asText();
		ObjectNode contents = (ObjectNode) src.get("contents");
		src.setAll(contents);
		src.put("path", groupPath);
		long id = src.get("id").asLong();
		ctx.legacyGroupIds.put(id, groupPath);
		
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
				statement.put("extraGroupName", ctx.legacyGroupIds.get(extraGroupId));
			}
			if (statement.has("fixedAttribute-attributeId"))
			{
				long attrId = statement.get("fixedAttribute-attributeId").asLong();
				long groupId = statement.get("fixedAttribute-attributeGroupId").asLong();
				String values = statement.get("fixedAttribute-attributeValues").asText();
				
				String group = ctx.legacyGroupIds.get(groupId);
				ObjectNode atDef = ctx.attributeTypesById.get(attrId);
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
			if (JsonUtil.notNull(gMember, "contents"))
			{
				String contents64 = gMember.get("contents").asText();
				String contents = new String(Base64.getDecoder().decode(contents64), 
						StandardCharsets.UTF_8);
				newMember.setAll((ObjectNode)objectMapper.readTree(contents));
			}
		}
	}
	
	
	private void updateAttributes(ObjectNode contents, UpdateContext ctx) throws IOException
	{
		ArrayNode attributes = (ArrayNode) contents.get("attributes");
		if (attributes == null)
			return;
		for (JsonNode node: attributes)
			updateStoredAttribute((ObjectNode) node, ctx);
	}
	
	private void updateStoredAttribute(ObjectNode src, UpdateContext ctx) throws IOException
	{
		String attr = src.get("attributeName").asText();
		String group = src.get("groupPath").asText();
		String values = src.remove("values").asText();
		ObjectNode atDef = ctx.attributeTypesByName.get(attr);
		String attributeSyntax = atDef.get("valueSyntaxId").asText();

		toNewAttribute(values, src, attr, group, attributeSyntax);
		src.put("entityId", src.get("entity").asLong());
		
		if (attr.startsWith("sys:Credential:"))
			updateStoredPassword(src);
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
			return convertLegacyEmailAttributeValue(binaryValue);
		case "jpegImage":
			return Base64.getEncoder().encodeToString(binaryValue);
		default:
			throw new IllegalStateException("Unknown attribute value type, can't be converted: " 
					+ valueSyntax);
		}
	}
	
	private String convertLegacyEmailAttributeValue(byte[] binaryValue) throws IOException
	{
		ObjectNode jsonN = (ObjectNode) objectMapper.readTree(new String(binaryValue, 
				StandardCharsets.UTF_8));
		JsonNode confirmationNode = jsonN.get("confirmationData");
		if (confirmationNode != null && !confirmationNode.isNull())
		{
			JsonNode parsedConfirmation = objectMapper.readTree(confirmationNode.asText());
			jsonN.set("confirmationData", parsedConfirmation);
		}
		return JsonUtil.serialize(jsonN);
	}
	
	/* legacy format of individual password:
	 * {
	 * 	"hash":"/pstb6lKuQYu0v/OTh+u3eR7u/2FAFYVjrX1UGbkDcE=",
	 * 	"salt":"7468058266641201959",
	 * 	"time":1491996617612,
	 * 	"rehashNumber":2233
	 * }
	 */
	private void updateStoredPassword(ObjectNode src) throws IOException
	{
		ArrayNode values = (ArrayNode) src.remove("values");
		String pNodeStr = values.get(0).asText();
		ObjectNode passwordNode = (ObjectNode) objectMapper.readTree(pNodeStr);
		
		ArrayNode passwords = (ArrayNode) passwordNode.remove("passwords");
		ArrayNode newPasswords = passwordNode.withArray("passwords");
		for (JsonNode password: passwords)
		{
			byte[] hash = password.get("hash").binaryValue();
			long salt = password.get("salt").asLong();
			long time = password.get("time").asLong();
			int rehashNum = 1;
			if (password.has("rehashNumber"))
				rehashNum = password.get("rehashNumber").asInt();
			byte[] saltBytes = ("" + salt).getBytes(StandardCharsets.UTF_8);
			ObjectNode newPass = toNewPasswordInfo(hash, saltBytes, rehashNum, time);
			newPasswords.add(newPass);
		}
		
		if (passwordNode.has("answerHash"))
		{
			byte[] answerHash = passwordNode.remove("answerHash").binaryValue();
			int rehashNum = 1;
			if (passwordNode.has("answerRehashNumber"))
				rehashNum = passwordNode.remove("answerRehashNumber").asInt();
			ObjectNode answer = toNewPasswordInfo(answerHash, null, rehashNum, null);
			passwordNode.set("answer", answer);
		}

		if (passwordNode.has("question"))
		{
			String question = passwordNode.remove("question").asText();
			passwordNode.put("securityQuestion", question);
		}
		
		ArrayNode newValues = src.withArray("values");
		String serialized = objectMapper.writeValueAsString(passwordNode);
		newValues.add(serialized);
	}

	private ObjectNode toNewPasswordInfo(byte[] hash, byte[] salt, int rehashNum, Long time)
	{
		ObjectNode pi = objectMapper.createObjectNode();
		pi.put("method", "SHA256");
		pi.put("hash", hash);
		
		ObjectNode params = pi.with("methodParams");
		params.put("rehashNumber", rehashNum);
		if (salt != null)
			pi.put("salt", salt);
		if (time != null)
			pi.put("time", time);
		return pi;
	}

	
	private static class UpdateContext
	{
		private Map<Long, ObjectNode> attributeTypesById = new HashMap<>();
		private Map<String, ObjectNode> attributeTypesByName = new HashMap<>();
		private Map<Long, String> legacyGroupIds = new HashMap<>();
	}
}
