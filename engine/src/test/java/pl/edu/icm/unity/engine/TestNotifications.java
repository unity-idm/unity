/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.engine.api.notification.NotificationProducer;
import pl.edu.icm.unity.engine.api.notification.NotificationStatus;
import pl.edu.icm.unity.engine.notifications.EmailFacility;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttribute;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttributeSyntax;
import pl.edu.icm.unity.stdext.credential.CredentialResetTemplateDefBase;
import pl.edu.icm.unity.stdext.credential.pass.EmailPasswordResetTemplateDef;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.utils.ContactEmailMetadataProvider;
import pl.edu.icm.unity.types.I18nMessage;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import pl.edu.icm.unity.types.basic.MessageTemplate;
import pl.edu.icm.unity.types.basic.MessageType;
import pl.edu.icm.unity.types.basic.NotificationChannel;

/**
 * Tests the core notifications mechanism and the email facility.
 * @author K. Benedyczak
 */
public class TestNotifications extends DBIntegrationTestBase
{
	@Autowired
	private NotificationProducer notProducer;
	
	
	//@Test
	public void testEmailNotification() throws Exception
	{
		String emailCfg = "mail.from=...\n" +
				"mail.smtp.host=...\n" +
				"mail.smtp.starttls.enable=true\n" +
				"mail.smtp.port=587\n" + //or 25
				"mailx.smtp.auth.username=...\n" +
				"mailx.smtp.auth.password=...\n" +
				"mail.smtp.auth=true\n" +
				"mail.smtp.timeoutSocket=15000\n" +
				"mail.smtp.connectiontimeout=15000\n" +
				"mailx.smtp.trustAll=true";
		String destinationAddress = "test@test.com";
		
		notMan.addNotificationChannel(new NotificationChannel("ch1", "", emailCfg, EmailFacility.NAME));
		EntityParam admin = new EntityParam(new IdentityTaV(UsernameIdentity.ID, "admin"));
		
		AttributeType attributeType = new AttributeType("email", VerifiableEmailAttributeSyntax.ID);
		Map<String, String> meta = new HashMap<>();
		meta.put(ContactEmailMetadataProvider.NAME, "");
		attributeType.setMetadata(meta);
		attributeType.setMinElements(1);
		
		aTypeMan.addAttributeType(attributeType);

		messageTemplateMan.addTemplate(new MessageTemplate(EmailPasswordResetTemplateDef.NAME,
				"", new I18nMessage(new I18nString("x"), new I18nString("x")), EmailPasswordResetTemplateDef.NAME,
				MessageType.PLAIN, "ch1"));

		Map<String, String> params = new HashMap<String, String>();
		params.put(CredentialResetTemplateDefBase.VAR_CODE, "AAAA");
		params.put(CredentialResetTemplateDefBase.VAR_USER, "some user");
		
		try
		{
			notProducer.sendNotification(admin, EmailPasswordResetTemplateDef.NAME, params, null, null, false);
			fail("Managed to send email for an entity without email attribute");
		} catch(IllegalIdentityValueException e){}

		Attribute emailA = VerifiableEmailAttribute.of("email", 
				"/", destinationAddress);
		attrsMan.createAttribute(admin, emailA);
		
		Future<NotificationStatus> statusFuture = notProducer.sendNotification(admin, 
				EmailPasswordResetTemplateDef.NAME, params, null, null, false);
		NotificationStatus status = statusFuture.get();
		if (!status.isSuccessful())
			status.getProblem().printStackTrace();
		assertTrue(status.isSuccessful());
	}
	
	
	@Test
	public void testManagement() throws Exception
	{
		
		for (String channel : notMan.getNotificationChannels().keySet())
			notMan.removeNotificationChannel(channel);
		
		String emailCfg = "";
		String emailCfg2 = "a=b";
		assertEquals(3, notMan.getNotificationFacilities().size());
		assertTrue(notMan.getNotificationFacilities().contains(EmailFacility.NAME));
		assertEquals(0, notMan.getNotificationChannels().size());
		notMan.addNotificationChannel(new NotificationChannel("ch1", "", emailCfg, EmailFacility.NAME));
		Map<String, NotificationChannel> channels = notMan.getNotificationChannels();
		assertEquals(1, channels.size());
		assertTrue(channels.containsKey("ch1"));
		assertEquals(emailCfg, channels.get("ch1").getConfiguration());
		
		try
		{
			notMan.updateNotificationChannel("wrong", emailCfg2);
			fail("Managed to update not existing channel");
		} catch (IllegalArgumentException e)
		{
		}
		notMan.updateNotificationChannel("ch1", emailCfg2);
		channels = notMan.getNotificationChannels();
		assertEquals(1, channels.size());
		assertTrue(channels.containsKey("ch1"));
		assertEquals(emailCfg2, channels.get("ch1").getConfiguration());

		try
		{
			notMan.removeNotificationChannel("wrong");
			fail("Managed to remove not existing channel");
		} catch (IllegalArgumentException e)
		{
		}
		notMan.removeNotificationChannel("ch1");
		assertEquals(0, notMan.getNotificationChannels().size());

		notMan.addNotificationChannel(new NotificationChannel("ch1", "", emailCfg, EmailFacility.NAME));
		channels = notMan.getNotificationChannels();
		assertEquals(1, channels.size());
		assertTrue(channels.containsKey("ch1"));
		assertEquals(emailCfg, channels.get("ch1").getConfiguration());
	}
}
