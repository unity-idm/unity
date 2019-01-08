package pl.edu.icm.unity.engine;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.msgtemplates.GenericMessageTemplateDef;
import pl.edu.icm.unity.base.msgtemplates.confirm.EmailConfirmationTemplateDef;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.builders.NotificationChannelBuilder;
import pl.edu.icm.unity.engine.msgtemplate.MessageTemplateProcessor;
import pl.edu.icm.unity.engine.notifications.sms.SMSFacility;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.I18nMessage;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.MessageTemplate;
import pl.edu.icm.unity.types.basic.MessageType;

public class TestMessageTemplates extends DBIntegrationTestBase
{
	@Autowired
	private MessageTemplateManagement msgTempMan;
	
	
	@Test
	public void testPersistence() throws Exception
	{
		assertEquals(2, msgTempMan.listTemplates().size());
		I18nString subject = new I18nString("stest");
		subject.addValue("pl", "Tytuł");
		subject.addValue("en", "Title");
		I18nString body = new I18nString("btest");
		body.addValue("pl", "Tekst");
		I18nMessage imsg = new I18nMessage(subject, body);
		MessageTemplate template = new MessageTemplate("tName", "tDesc", imsg,
				"EmailPasswordResetCode", MessageType.PLAIN,
				UnityServerConfiguration.DEFAULT_EMAIL_CHANNEL);
		msgTempMan.addTemplate(template);
		assertEquals(3, msgTempMan.listTemplates().size());
		MessageTemplate added = msgTempMan.getTemplate("tName");
		assertEquals("tName", added.getName());
		assertEquals("tDesc", added.getDescription());
		assertEquals("EmailPasswordResetCode", added.getConsumer());
		assertEquals(subject, added.getMessage().getSubject());
		assertEquals(body, added.getMessage().getBody());
		
		I18nMessage imsg2 = new I18nMessage(new I18nString("stest${code}"), new I18nString("btest${code}"));
		template.setMessage(imsg2);	
		msgTempMan.updateTemplate(template);
		
		Map<String, String> params = new HashMap<>();
		params.put("code", "svalue");
		added = msgTempMan.getTemplate("tName");
		assertEquals("stestsvalue", new MessageTemplateProcessor().getMessage(added, "pl", "en", params, 
				Collections.emptyMap()).getSubject());
		assertEquals("btestsvalue", new MessageTemplateProcessor().getMessage(added, null, "en", params, 
				Collections.emptyMap()).getBody());
		
		msgTempMan.removeTemplate("tName");
		assertEquals(2, msgTempMan.listTemplates().size());		
	}
	
	@Test
	public void testValidationConsumer() throws EngineException 
	{
		I18nMessage imsg = new I18nMessage(new I18nString("stest"), new I18nString("btest"));
		MessageTemplate template = new MessageTemplate("tName", "tDesc", imsg,
				"FailConsumer", MessageType.PLAIN,
				UnityServerConfiguration.DEFAULT_EMAIL_CHANNEL);
		try
		{
			msgTempMan.addTemplate(template);
			fail("Exception not throw.");
		} catch (IllegalArgumentException e)
		{
		}
		
	}
	
	@Test
	public void testValidationMessage() throws EngineException 
	{
		I18nMessage imsg = new I18nMessage(new I18nString("stest${code}"), new I18nString("btest"));
		MessageTemplate template = new MessageTemplate("tName", "tDesc", imsg, "RejectForm",
				MessageType.PLAIN, UnityServerConfiguration.DEFAULT_EMAIL_CHANNEL);
		try
		{
			msgTempMan.addTemplate(template);
			fail("Exception not throw.");
		} catch (IllegalArgumentException e)
		{
		}
	}
	
	@Test
	public void incompatibleChannelShouldThrowException() throws EngineException
	{

		try
		{
			notMan.removeNotificationChannel(
					UnityServerConfiguration.DEFAULT_SMS_CHANNEL);
		} catch (Exception e)
		{
		}

		notMan.addNotificationChannel(NotificationChannelBuilder.notificationChannel()
				.withName(UnityServerConfiguration.DEFAULT_SMS_CHANNEL)
				.withDescription("")
				.withConfiguration("unity.sms.provider=clickatell\n"
						+ "unity.sms.clickatell.apiKey=xx\n"
						+ "unity.sms.clickatell.charset=ASCII")
				.withFacilityId(SMSFacility.NAME).build());

		I18nMessage imsg = new I18nMessage(new I18nString("stest"),
				new I18nString("btest"));
		MessageTemplate template = new MessageTemplate("tName", "tDesc", imsg,
				EmailConfirmationTemplateDef.NAME, MessageType.PLAIN,
				UnityServerConfiguration.DEFAULT_SMS_CHANNEL);
		try
		{
			msgTempMan.addTemplate(template);
			fail("Exception not throw.");
		} catch (WrongArgumentException e)
		{
		}
	}
	
	@Test
	public void ifNotExistingChannelShouldThrowException() throws EngineException 
	{
	
		I18nMessage imsg = new I18nMessage(new I18nString("stest"), new I18nString("btest"));
		MessageTemplate template = new MessageTemplate("tName", "tDesc", imsg, EmailConfirmationTemplateDef.NAME,
				MessageType.PLAIN, "ch1");
		try
		{
			msgTempMan.addTemplate(template);
			fail("Exception not throw.");
		} catch (WrongArgumentException e)
		{
		}
	}
	
	@Test
	public void shouldAddGenericTemplateWithEmptyChannel() throws EngineException 
	{
	
		I18nMessage imsg = new I18nMessage(new I18nString("stest"), new I18nString("btest"));
		MessageTemplate template = new MessageTemplate("tName", "tDesc", imsg, GenericMessageTemplateDef.NAME,
				MessageType.PLAIN, null);
		
		msgTempMan.addTemplate(template);
		assertThat(msgTempMan.getTemplate("tName").getConsumer(), is(GenericMessageTemplateDef.NAME));		
	}
}
