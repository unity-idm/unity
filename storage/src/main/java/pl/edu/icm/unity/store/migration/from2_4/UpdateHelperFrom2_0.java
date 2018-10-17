/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.from2_4;


import java.io.IOException;
import java.util.Optional;

import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.objstore.reg.eform.EnquiryFormHandler;

/**
 * Database update helper. If object is update then non empty optional is return;
 * @author P.Piernik
 *
 */
public class UpdateHelperFrom2_0
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, UpdateHelperFrom2_0.class);
	
	public static Optional<ObjectNode> dropChannelFromGenericForm(ObjectNode objContent, String type)
	{
		ObjectNode notCfg = (ObjectNode) objContent
				.get("NotificationsConfiguration");
		if (notCfg.has("channel"))
		{
			notCfg.remove("channel");
			log.info("Removing notification channel from {} {}", getName(objContent),
					type == EnquiryFormHandler.ENQUIRY_FORM_OBJECT_TYPE
							? "enquiry form"
							: "registration form");		
			return Optional.of(objContent);	
		}
		
		return Optional.empty();
	}

	public static Optional<ObjectNode> updateNotificationChannel(ObjectNode objContent)
	{
		String newName = null;
		if (objContent.get("name").asText().equals("Default e-mail channel"))
		{
			newName = "default_email";
		}

		if (objContent.get("name").asText().equals("Default SMS channel"))
		{
			newName = "default_sms";
		}

		if (newName != null)
		{
			log.info("Updating notification channel {}, changing name to {}",
					getName(objContent), newName);
			objContent.put("name", newName);
			return Optional.of(objContent);
		}

		return Optional.empty();
	}
	
	public static Optional<ObjectNode> updateInvitationWithCode(ObjectNode objContent)
	{
		if (objContent.has("channelId")) 
		{
			objContent.remove("channelId");
			log.info("Removing channelId from invitation {}",  getName(objContent));
		}
		
		return Optional.empty();
	}

	public static Optional<ObjectNode> updateMessageTemplates(ObjectNode objContent)
	{

		String name = objContent.get("name").asText();
		boolean update = false;
		if (objContent.get("consumer").asText().equals("Confirmation"))
		{
			log.info("Updating consumer from {} to {} in message template {}",
					"Confirmation", "EmailConfirmation", name);
			objContent.put("consumer", "EmailConfirmation");
			update = true;
		}

		if (objContent.get("consumer").asText().equals("PasswordResetCode"))
		{
			log.info("Updating consumer from {} to {} in message template {}",
					"PasswordResetCode", "EmailPasswordResetCode",
					name);
			objContent.put("consumer", "EmailPasswordResetCode");
			update = true;
		}

		if (!objContent.get("consumer").asText().equals("Generic"))
		{
			log.info("Setting notificationChannel to default_email in message template {}",
					name);
			objContent.put("notificationChannel", "default_email");
			update = true;

		}
		
		if (update)
			return Optional.of(objContent);
		
		return Optional.empty();
	}
	
	
	public static Optional<ObjectNode> updateCredentialsDefinition(ObjectNode objContent)
	{
		if (objContent.get("typeId").asText().equals("password"))
		{
			if (updateResetSettings(objContent, getName(objContent)))
				return Optional.of(objContent);
		}
		
		return Optional.empty();
	}
	
	private static boolean updateResetSettings(ObjectNode content, String name)
	{
		ObjectNode configurationNode = null;
		try
		{
			configurationNode = (ObjectNode) Constants.MAPPER
					.readTree(content.get("configuration").asText());
		} catch (IOException e)
		{
			log.warn("Can't update credential reset settings, skipping it", e);
			return false;
		}

		if (configurationNode.get("resetSettings") != null)
		{

			ObjectNode reset = (ObjectNode) configurationNode.get("resetSettings");
			if (reset.has("requireEmailConfirmation"))
			{
				boolean reqEmail = reset.get("requireEmailConfirmation")
						.asBoolean();
				reset.remove("requireEmailConfirmation");
				if (reqEmail)
				{
					log.info("Setting confirmationMode in credential {} to {}",
							name, "RequireEmail");
					reset.put("confirmationMode", "RequireEmail");
					if (reset.get("securityCodeMsgTemplate") != null)
					{

						log.info("Setting emailSecurityCodeMsgTemplate in credential {} to {}",
								name,
								reset.get("securityCodeMsgTemplate")
										.asText());
						reset.put("emailSecurityCodeMsgTemplate",
								reset.get("securityCodeMsgTemplate")
										.asText());
						reset.remove("securityCodeMsgTemplate");
					}
				} else
				{
					log.info("Setting confirmationMode in credential {} to {}",
							name, "NothingRequire");
					reset.put("confirmationMode", "NothingRequire");
				}
				configurationNode.set("resetSettings", reset);
				content.put("configuration", configurationNode.toString());
				return true;
			}
		}

		return false;

	}
	private static final String getName(ObjectNode objContent)
	{
		if (objContent.has("name"))
			return objContent.get("name").asText();
		if (objContent.has("Name"))
			return objContent.get("Name").asText();
		
		return objContent.toString();
	}
}
