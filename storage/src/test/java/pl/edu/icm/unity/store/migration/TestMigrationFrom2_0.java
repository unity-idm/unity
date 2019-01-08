/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.migration;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.store.StorageCleanerImpl;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.api.IdentityTypeDAO;
import pl.edu.icm.unity.store.api.ImportExport;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.store.impl.objstore.GenericObjectBean;
import pl.edu.icm.unity.store.impl.objstore.ObjectStoreDAO;
import pl.edu.icm.unity.store.objstore.cred.CredentialHandler;
import pl.edu.icm.unity.store.objstore.msgtemplate.MessageTemplateHandler;
import pl.edu.icm.unity.store.objstore.notify.NotificationChannelHandler;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.IdentityType;
import pl.edu.icm.unity.types.basic.MessageTemplate;
import pl.edu.icm.unity.types.basic.NotificationChannel;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath*:META-INF/components.xml"})
public class TestMigrationFrom2_0
{
	@Autowired
	protected StorageCleanerImpl dbCleaner;

	@Autowired
	protected TransactionalRunner tx;
	
	@Autowired
	protected ImportExport ie;
	
	@Autowired
	private ObjectStoreDAO genericDao;
	
	@Autowired
	private AttributeTypeDAO atDAO;
	@Autowired
	private IdentityTypeDAO itDAO;
	
	
	@Before
	public void cleanDB()
	{
		dbCleaner.reset();
	}

	@Test
	public void testImportFrom2_0_0()
	{
		tx.runInTransaction(() -> {
			try
			{
				ie.load(new BufferedInputStream(new FileInputStream(
						"src/test/resources/updateData/from2.4.x/"
								+ "testbed-from2.4.0-confirmationConfig.json")));
				ie.store(new FileOutputStream("target/afterImport.json"));
			} catch (Exception e)
			{
				e.printStackTrace();
				fail("Import failed " + e);
			}

			checkAttributeType();
			checkIdentityType();
			checkMessageTemplate();
			checkNotificationChannel();
			checkConfirmationConfiguration();
			checkCredentialResetSettings();
		});
	}
	
	private void checkAttributeType()
	{
		assertThat(atDAO.getAll().size(), is(67));
		AttributeType emailAttrType = atDAO.get("email");
		assertThat(emailAttrType.getValueSyntaxConfiguration()
				.get("messageTemplate").asText(), is("emailConfirmation"));
		assertThat(emailAttrType.getValueSyntaxConfiguration().get("validityTime")
				.asInt(), is(2880));
	}
	
	private void checkIdentityType()
	{
		assertThat(itDAO.getAll().size(), is(7));	
		IdentityType emailIdentityType = itDAO.get("email");
		assertThat(emailIdentityType.getEmailConfirmationConfiguration()
				.getMessageTemplate(), is("emailConfirmation"));
		assertThat(emailIdentityType.getEmailConfirmationConfiguration()
				.getValidityTime(), is(2880));
	}
	
	private void checkMessageTemplate()
	{
		assertThat(genericDao.getObjectsOfType(
				MessageTemplateHandler.MESSAGE_TEMPLATE_OBJECT_TYPE).size(),
				is(11));

		for (GenericObjectBean msg : genericDao.getObjectsOfType(
				MessageTemplateHandler.MESSAGE_TEMPLATE_OBJECT_TYPE))
		{
			MessageTemplate template = null;
			try
			{
				template = new MessageTemplate((ObjectNode) Constants.MAPPER
						.readTree(msg.getContents()));
			} catch (IOException e)
			{
				e.printStackTrace();
				fail("Import failed " + e);
			}
			if (!template.getConsumer().equals("Generic"))
				assertThat(template.getNotificationChannel(),
						is("default_email"));

			if (template.getName().equals("emailConfirmation"))
				assertThat(template.getConsumer(), is("EmailConfirmation"));
		}
	}
	
	private void checkNotificationChannel()
	{
		assertThat(genericDao.getObjectsOfType(
				NotificationChannelHandler.NOTIFICATION_CHANNEL_ID).size(),
				is(1));

		for (GenericObjectBean rawChannel : genericDao.getObjectsOfType(
				NotificationChannelHandler.NOTIFICATION_CHANNEL_ID))
		{
			NotificationChannel channel = null;
			try
			{
				channel = new NotificationChannel(
						(ObjectNode) Constants.MAPPER.readTree(
								rawChannel.getContents()));
			} catch (IOException e)
			{
				e.printStackTrace();
				fail("Import failed " + e);
			}
			if (channel.getDescription().equals("Default email channel"))
				assertThat(channel.getName(), is("default_email"));
		}
	}
	
	private void checkConfirmationConfiguration()
	{
		assertThat(genericDao.getObjectsOfType("confirmationConfiguration").size(),
				is(0));
	}
	
	private void checkCredentialResetSettings()
	{
		assertThat(genericDao.getObjectsOfType(
				CredentialHandler.CREDENTIAL_OBJECT_TYPE).size(), 
				is(2));
		
		for (GenericObjectBean cred : genericDao
				.getObjectsOfType(CredentialHandler.CREDENTIAL_OBJECT_TYPE))
		{
			CredentialDefinition credential = null;
			try
			{
				credential = new CredentialDefinition(
						(ObjectNode) Constants.MAPPER.readTree(
								cred.getContents()));
				if (credential.getName().equals("newCredential"))
				{
					JsonNode config = Constants.MAPPER.readTree(
							credential.getConfiguration());
					
					JsonNode resetSet = config.get("resetSettings");				
					assertThat(resetSet.get("enable").asBoolean(),
							is(true));
					assertThat(resetSet.get("confirmationMode")
							.asText(), is("RequireEmail"));
					assertThat(resetSet
							.get("emailSecurityCodeMsgTemplate")
							.asText(), is("passwordResetCode"));
				}

			} catch (IOException e)
			{
				e.printStackTrace();
				fail("Import failed " + e);
			}
		}

	}
}
