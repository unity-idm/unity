/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.export.update;

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

import pl.edu.icm.unity.Constants;
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
 * Update db from 2.4.1 to 2.5.0
 * @author P.Piernik
 *
 */
@Component
public class UpdateFrom2_0_0 implements Update

{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, UpdateFrom2_0_0.class);
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
			if (objContent.get("typeId").asText().equals("password"))
				updateResetSettings(objContent);
		}	
		
	}
	
	private void updateResetSettings(ObjectNode content)
	{
		ObjectNode configurationNode = null;
		try
		{
			configurationNode = (ObjectNode) Constants.MAPPER
					.readTree(content.get("configuration").asText());
		} catch (IOException e)
		{
			log.warn("Can't update credential reset settings, skipping it", e);
			return;
		}

		if (configurationNode.get("resetSettings") != null)
		{

			ObjectNode reset = (ObjectNode) configurationNode.get("resetSettings");
			boolean reqEmail = reset.get("requireEmailConfirmation").asBoolean();
			reset.remove("requireEmailConfirmation");
			if (reqEmail)
			{
				reset.put("confirmationMode", "RequireEmail");
				if (reset.get("securityCodeMsgTemplate") != null)
				{
					reset.put("emailSecurityCodeMsgTemplate", reset
							.get("securityCodeMsgTemplate").asText());
					reset.remove("securityCodeMsgTemplate");
				}
			} else
			{
				reset.put("confirmationMode", "NothingRequire");
			}
			configurationNode.set("resetSettings", reset);
			content.put("configuration", configurationNode.toString());
		}
	}
	
	private void updateNotificationChannels(ObjectNode contents)
	{
		for (ObjectNode objContent : getGenericContent(contents,
				NotificationChannelHandler.NOTIFICATION_CHANNEL_ID))
		{
			if (objContent.get("name").asText().equals("Default e-mail channel"))
				objContent.put("name", "default_email");
			if (objContent.get("name").asText().equals("Default SMS channel"))
				objContent.put("name", "default_sms");
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
					}

			}
		}
		ArrayNode idTypes = (ArrayNode) contents.get("identityTypes");
		if (idTypes != null)

		{
			for (JsonNode node : idTypes)
			{
				String name = node.get("name").asText();
				EmailConfirmationConfiguration emailConfig = attrsConfig.get(name);
				if (emailConfig != null)
					if (node.get("identityTypeProvider").asText()
							.equals("email"))

					{
						ObjectNode nodeO = (ObjectNode) node;
						nodeO.set("emailConfirmationConfiguration",
								emailConfig.toJson());
					}
			}
		}
		
		contents.remove("confirmationConfiguration");
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
			if (objContent.has("channelId"))
				objContent.remove("channelId");
		}
	}

	private void updateMessageTemplates(ObjectNode contents)
	{
		for (ObjectNode objContent : getGenericContent(contents,
				MessageTemplateHandler.MESSAGE_TEMPLATE_OBJECT_TYPE))
		{
			if (objContent.get("consumer").asText().equals("Confirmation"))
			{
				objContent.put("consumer", "EmailConfirmation");
			}
			
			if (objContent.get("consumer").asText().equals("PasswordResetCode"))
			{
				objContent.put("consumer", "EmailPasswordResetCode");
			}
			
			if (!objContent.get("consumer").asText().equals("Generic"))
				objContent.put("notificationChannel", "default_email");
		}
	}

	private void updateGenericForm(ObjectNode contents, String fromType) throws IOException
	{
		for (ObjectNode objContent : getGenericContent(contents, fromType))
		{
			ObjectNode notCfg = (ObjectNode) objContent
					.get("NotificationsConfiguration");
			if (notCfg.has("channel"))
				notCfg.remove("channel");
		}
	}

	
	private void updateEmailIdentitiesCmpValueToLowercase(ObjectNode contents)
	{
		ArrayNode ids = (ArrayNode) contents.get("identities");
		if (ids == null)
			return;
		for (JsonNode node: ids)
			updateEmailIdentityCmpValueToLowercase((ObjectNode) node);
	}

	private void updateEmailIdentityCmpValueToLowercase(ObjectNode src)
	{
		String type = src.get("typeId").asText();
		if (!"email".equals(type))
			return;
		
		String value = src.get("value").asText();
		String updated = new VerifiableEmail(value).getComparableValue();
		
		src.put("comparableValue", updated);
	}
}
