/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.from2_4;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.impl.attributetype.AttributeTypeBean;
import pl.edu.icm.unity.store.impl.attributetype.AttributeTypesMapper;
import pl.edu.icm.unity.store.impl.identities.IdentitiesMapper;
import pl.edu.icm.unity.store.impl.identities.IdentityBean;
import pl.edu.icm.unity.store.impl.identitytype.IdentityTypesMapper;
import pl.edu.icm.unity.store.impl.objstore.GenericObjectBean;
import pl.edu.icm.unity.store.impl.objstore.ObjectStoreDAO;
import pl.edu.icm.unity.store.migration.InDBSchemaUpdater;
import pl.edu.icm.unity.store.objstore.cred.CredentialHandler;
import pl.edu.icm.unity.store.objstore.msgtemplate.MessageTemplateHandler;
import pl.edu.icm.unity.store.objstore.notify.NotificationChannelHandler;
import pl.edu.icm.unity.store.objstore.reg.eform.EnquiryFormHandler;
import pl.edu.icm.unity.store.objstore.reg.form.RegistrationFormHandler;
import pl.edu.icm.unity.store.objstore.reg.invite.InvitationHandler;
import pl.edu.icm.unity.store.rdbms.BaseBean;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionTL;
import pl.edu.icm.unity.store.types.StoredIdentity;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.IdentityType;
import pl.edu.icm.unity.types.basic.VerifiableEmail;
import pl.edu.icm.unity.types.confirmation.EmailConfirmationConfiguration;

/**
 * Update db to 2.5.0 release version (DB schema version 2.3) from previous versions (schema 2.2)
 * @author P.Piernik
 */
@Component
public class InDBUpdateFromSchema2_2 implements InDBSchemaUpdater
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, InDBUpdateFromSchema2_2.class);

	@Autowired
	private ObjectStoreDAO genericObjectsDAO;
	
	@Override
	public void update() throws IOException
	{
		updateEmailIdentitiesCmpValueToLowercase();
		dropChannelFromGenericForm(RegistrationFormHandler.REGISTRATION_FORM_OBJECT_TYPE);
		dropChannelFromGenericForm(EnquiryFormHandler.ENQUIRY_FORM_OBJECT_TYPE);

		updateInvitationWithCode();
		updateNotificationChannels();
		updateMessageTemplates();
		updateCredentialsDefinition();
		moveConfirmationConfiguration();
	}
	
	private void dropChannelFromGenericForm(String objType)
	{
		List<GenericObjectBean> forms = genericObjectsDAO.getObjectsOfType(objType);
		for (GenericObjectBean form : forms)
		{
			ObjectNode objContent = JsonUtil.parse(form.getContents());
			if (UpdateHelperFrom2_0.dropChannelFromGenericForm(objContent, objType).isPresent())
			{
				form.setContents(JsonUtil.serialize2Bytes(objContent));
				genericObjectsDAO.updateByKey(form.getId(), form);
			}
		}
	}

	private void updateNotificationChannels()
	{

		List<GenericObjectBean> notChannels = genericObjectsDAO.getObjectsOfType(
				NotificationChannelHandler.NOTIFICATION_CHANNEL_ID);
		for (GenericObjectBean channel : notChannels)
		{
			
			ObjectNode objContent = JsonUtil.parse(channel.getContents());

			if (UpdateHelperFrom2_0.updateNotificationChannel(objContent).isPresent())
			{	
				channel.setContents(JsonUtil.serialize2Bytes(objContent));
				genericObjectsDAO.updateByKey(channel.getId(), channel);
			}
		}
	}

	private void moveConfirmationConfiguration()
	{
		Map<String, EmailConfirmationConfiguration> attrsConfig = new HashMap<>();
		Map<String, EmailConfirmationConfiguration> idsConfig = new HashMap<>();

		List<GenericObjectBean> conConfs = genericObjectsDAO
				.getObjectsOfType("confirmationConfiguration");
		for (GenericObjectBean confirmationConfig : conConfs)
		{
			ObjectNode objContent = JsonUtil.parse(confirmationConfig.getContents());

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

		updateAttributeTypes(attrsConfig);
		updateIdentityTypes(idsConfig);

		log.info("Removing all confirmationConfiguration objects");
		genericObjectsDAO.removeObjectsByType("confirmationConfiguration");
	}

	private void updateAttributeTypes(Map<String, EmailConfirmationConfiguration> attrsConfig)
	{
		AttributeTypesMapper attributeTypesMapper = SQLTransactionTL.getSql()
				.getMapper(AttributeTypesMapper.class);
		List<AttributeTypeBean> allAttrTypes = attributeTypesMapper.getAll();
		for (AttributeTypeBean attr : allAttrTypes)
		{
			EmailConfirmationConfiguration emailConfig = attrsConfig
					.get(attr.getName());
			if (emailConfig != null
					&& attr.getValueSyntaxId().equals("verifiableEmail"))
			{
				AttributeType at = new AttributeType();
				at.setName(attr.getName());
				at.setValueSyntax(attr.getValueSyntaxId());
				at.fromJsonBase(JsonUtil.parse(attr.getContents()));
				ObjectNode main = Constants.MAPPER.createObjectNode();
				main.set("emailConfirmationConfiguration", emailConfig.toJson());
				at.setValueSyntaxConfiguration(main);
				attr.setContents(JsonUtil.serialize2Bytes(at.toJsonBase()));
				log.info("Updating attribute type {}, setting confirmationConfiguration to {}",
						at.toJsonBase(), emailConfig.toJson());
				attributeTypesMapper.updateByKey(attr);
			}
		}
	}

	private void updateIdentityTypes(Map<String, EmailConfirmationConfiguration> idsConfig)
	{
		IdentityTypesMapper identityTypesMapper = SQLTransactionTL.getSql()
				.getMapper(IdentityTypesMapper.class);
		List<BaseBean> allIdTypes = identityTypesMapper.getAll();
		for (BaseBean id : allIdTypes)
		{
			EmailConfirmationConfiguration emailConfig = idsConfig.get(id.getName());
			if (emailConfig != null)
			{
				IdentityType it = new IdentityType(id.getName());
				it.fromJsonBase(JsonUtil.parse(id.getContents()));
				if (it.getIdentityTypeProvider().equals("email"))
				{
					it.setEmailConfirmationConfiguration(emailConfig);
					id.setContents(JsonUtil.serialize2Bytes(it.toJsonBase()));
					log.info("Updating identity type {}, setting confirmationConfiguration to {}",
							id.getName(), emailConfig.toJson());
					identityTypesMapper.updateByKey(id);

				}
			}
		}

	}

	private void updateMessageTemplates()
	{
		List<GenericObjectBean> msgs = genericObjectsDAO.getObjectsOfType(
				MessageTemplateHandler.MESSAGE_TEMPLATE_OBJECT_TYPE);
		for (GenericObjectBean msg : msgs)
		{
			ObjectNode objContent = JsonUtil.parse(msg.getContents());

			if (UpdateHelperFrom2_0.updateMessageTemplates(objContent).isPresent())
			{
				msg.setContents(JsonUtil.serialize2Bytes(objContent));
				log.info("Updating message template {}", msg.getName());
				genericObjectsDAO.updateByKey(msg.getId(), msg);
			}

		}
	}

	private void updateCredentialsDefinition()
	{

		List<GenericObjectBean> creds = genericObjectsDAO
				.getObjectsOfType(CredentialHandler.CREDENTIAL_OBJECT_TYPE);

		for (GenericObjectBean cred : creds)
		{
			ObjectNode objContent = JsonUtil.parse(cred.getContents());

			if (UpdateHelperFrom2_0.updateCredentialsDefinition(objContent).isPresent())
			{
				cred.setContents(JsonUtil.serialize2Bytes(objContent));
				genericObjectsDAO.updateByKey(cred.getId(), cred);
			}
		}
	}

	

	private void updateInvitationWithCode()
	{
		List<GenericObjectBean> invs = genericObjectsDAO
				.getObjectsOfType(InvitationHandler.INVITATION_OBJECT_TYPE);
		for (GenericObjectBean inv : invs)
		{
			ObjectNode objContent = JsonUtil.parse(inv.getContents());
			if (UpdateHelperFrom2_0.updateInvitationWithCode(objContent).isPresent())
			{
				inv.setContents(JsonUtil.serialize2Bytes(objContent));
				genericObjectsDAO.updateByKey(inv.getId(), inv);
			}
		}
	}


	private void updateEmailIdentitiesCmpValueToLowercase()
	{
		IdentitiesMapper identityMapper = SQLTransactionTL.getSql()
				.getMapper(IdentitiesMapper.class);
		List<IdentityBean> allIdentities = identityMapper.getAll();
		for (IdentityBean id : allIdentities)
		{
			if (id.getTypeName().equals("email"))
			{
				String value = new String(id.getContents(), StandardCharsets.UTF_8);
				String updated = new VerifiableEmail(value).getComparableValue();
				String inDbUpdated = StoredIdentity.toInDBIdentityValue("email",
						updated);
				if (!inDbUpdated.equals(id.getName()))
				{
					log.info("Updating email identity cmp value to lowercase {} -> {}",
							id.getName(), inDbUpdated);
					id.setName(inDbUpdated);
					identityMapper.updateByKey(id);
				}
			}
		}
	}
}
