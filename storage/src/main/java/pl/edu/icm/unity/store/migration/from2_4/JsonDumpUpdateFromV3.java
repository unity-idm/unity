/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.from2_4;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.export.Update;
import pl.edu.icm.unity.store.objstore.cred.CredentialHandler;
import pl.edu.icm.unity.store.objstore.msgtemplate.MessageTemplateHandler;
import pl.edu.icm.unity.store.objstore.notify.NotificationChannelHandler;
import pl.edu.icm.unity.store.objstore.reg.eform.EnquiryFormHandler;
import pl.edu.icm.unity.store.objstore.reg.form.RegistrationFormHandler;
import pl.edu.icm.unity.store.objstore.reg.invite.InvitationHandler;
import pl.edu.icm.unity.types.basic.VerifiableEmail;
import pl.edu.icm.unity.types.confirmation.EmailConfirmationConfiguration;

/**
 * Update db from 2.0-2.4 (Json schema V3) to 2.5.0+ (V4)
 * @author P.Piernik
 *
 */
@Component
public class JsonDumpUpdateFromV3 implements Update

{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, JsonDumpUpdateFromV3.class);
	@Autowired
	private ObjectMapper objectMapper;

	@Override
	public InputStream update(InputStream is) throws IOException
	{
		ObjectNode root = (ObjectNode) objectMapper.readTree(is);
		ObjectNode contents = (ObjectNode) root.get("contents");
		updateGenericForm(contents, EnquiryFormHandler.ENQUIRY_FORM_OBJECT_TYPE);
		updateGenericForm(contents, RegistrationFormHandler.REGISTRATION_FORM_OBJECT_TYPE);
		updateInvitationWithCode(contents);
		updateNotificationChannels(contents);
		updateMessageTemplates(contents);
		updateCredentialsDefinition(contents);
		
		moveConfirmationConfiguration(contents);

		updateEmailIdentitiesCmpValueToLowercase(contents);
		
		return new ByteArrayInputStream(objectMapper.writeValueAsBytes(root));
	}

	private void updateCredentialsDefinition(ObjectNode contents)
	{
		for (ObjectNode objContent : getGenericContent(contents,
				CredentialHandler.CREDENTIAL_OBJECT_TYPE))
		{
			UpdateHelperFrom2_0.updateCredentialsDefinition(objContent);
		}

	}

	private void updateNotificationChannels(ObjectNode contents)
	{
		for (ObjectNode objContent : getGenericContent(contents,
				NotificationChannelHandler.NOTIFICATION_CHANNEL_ID))
		{
			UpdateHelperFrom2_0.updateNotificationChannel(objContent);
		}
	}

	private void moveConfirmationConfiguration(ObjectNode contents)
	{
		Map<String, EmailConfirmationConfiguration> attrsConfig = new HashMap<>();
		Map<String, EmailConfirmationConfiguration> idsConfig = new HashMap<>();

		for (ObjectNode objContent : getGenericContent(contents,
				"confirmationConfiguration"))
		{
			EmailConfirmationConfiguration emailConfig = new EmailConfirmationConfiguration();
			emailConfig.setMessageTemplate(objContent.get("msgTemplate").asText());
			if (objContent.get("validityTime") != null)
				emailConfig.setValidityTime(objContent.get("validityTime").asInt());

			if (objContent.get("typeToConfirm").asText().equals("attribute"))
			{

				attrsConfig.put(objContent.get("nameToConfirm").asText(),
						emailConfig);

			} else if (objContent.get("typeToConfirm").asText().equals("identity"))
			{

				idsConfig.put(objContent.get("nameToConfirm").asText(),
						emailConfig);

			}
		}

		updateAttributeTypes(attrsConfig, contents);
		updateIdentityTypes(idsConfig, contents);

		log.info("Removing all confirmationConfiguration objects");
		contents.remove("confirmationConfiguration");
	}

	private void updateAttributeTypes(Map<String, EmailConfirmationConfiguration> attrsConfig,
			ObjectNode contents)
	{
		ArrayNode attrTypes = (ArrayNode) contents.get("attributeTypes");
		if (attrTypes != null)
		{
			for (JsonNode node : attrTypes)
			{
				String name = node.get("name").asText();
				EmailConfirmationConfiguration emailConfig = attrsConfig.get(name);
				if (emailConfig != null)
					if (node.get("syntaxId").asText().equals("verifiableEmail"))

					{
						ObjectNode nodeO = (ObjectNode) node;
						nodeO.set("syntaxState", emailConfig.toJson());
						log.info("Updating attribute type {}, setting confirmationConfiguration to {}",
								name, emailConfig.toJson());
					}

			}
		}
	}

	private void updateIdentityTypes(Map<String, EmailConfirmationConfiguration> idsConfig,
			ObjectNode contents)
	{
		ArrayNode idTypes = (ArrayNode) contents.get("identityTypes");
		if (idTypes != null)

		{
			for (JsonNode node : idTypes)
			{
				String name = node.get("name").asText();
				EmailConfirmationConfiguration emailConfig = idsConfig.get(name);
				if (emailConfig != null)
					if (node.get("identityTypeProvider").asText()
							.equals("email"))

					{
						ObjectNode nodeO = (ObjectNode) node;
						nodeO.set("emailConfirmationConfiguration",
								emailConfig.toJson());
						log.info("Updating identity type {}, setting confirmationConfiguration to {}",
								name, emailConfig.toJson());

					}
			}
		}
	}

	private Set<ObjectNode> getGenericContent(ObjectNode contents, String type)
	{
		Set<ObjectNode> ret = new HashSet<>();
		ArrayNode generics = (ArrayNode) contents.get(type);
		if (generics != null)
		{
			for (JsonNode obj : generics)
			{
				ret.add((ObjectNode) obj.get("obj"));
			}
		}
		return ret;
	}

	private void updateInvitationWithCode(ObjectNode contents)
	{
		for (ObjectNode objContent : getGenericContent(contents,
				InvitationHandler.INVITATION_OBJECT_TYPE))
		{
			UpdateHelperFrom2_0.updateInvitationWithCode(objContent);
		}
	}

	private void updateMessageTemplates(ObjectNode contents)
	{
		for (ObjectNode objContent : getGenericContent(contents,
				MessageTemplateHandler.MESSAGE_TEMPLATE_OBJECT_TYPE))
		{
			UpdateHelperFrom2_0.updateMessageTemplates(objContent);
		}
	}

	private void updateGenericForm(ObjectNode contents, String fromType) throws IOException
	{
		for (ObjectNode objContent : getGenericContent(contents, fromType))
		{
			UpdateHelperFrom2_0.dropChannelFromGenericForm(objContent, fromType);
		}
	}

	private void updateEmailIdentitiesCmpValueToLowercase(ObjectNode contents)
	{
		ArrayNode ids = (ArrayNode) contents.get("identities");
		if (ids == null)
			return;
		for (JsonNode node : ids)
			updateEmailIdentityCmpValueToLowercase((ObjectNode) node);
	}

	private void updateEmailIdentityCmpValueToLowercase(ObjectNode src)
	{
		String type = src.get("typeId").asText();
		if (!"email".equals(type))
			return;

		String value = src.get("value").asText();
		String updated = new VerifiableEmail(value).getComparableValue();
		log.info("Updating email cmp value to be lowercase {} -> {}",
				src.get("comparableValue"), updated);
		src.put("comparableValue", updated);
	}
}
