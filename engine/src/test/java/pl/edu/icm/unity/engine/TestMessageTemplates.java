package pl.edu.icm.unity.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.msgtemplates.MessageTemplate;
import pl.edu.icm.unity.msgtemplates.MessageTemplate.I18nMessage;
import pl.edu.icm.unity.server.api.MessageTemplateManagement;
import pl.edu.icm.unity.types.I18nString;

public class TestMessageTemplates extends DBIntegrationTestBase
{
	@Autowired
	MessageTemplateManagement msgTempMan;
	
	
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
		MessageTemplate template = new MessageTemplate("tName", "tDesc", imsg, "PasswordResetCode");
		msgTempMan.addTemplate(template);
		assertEquals(3, msgTempMan.listTemplates().size());
		MessageTemplate added = msgTempMan.getTemplate("tName");
		assertEquals("tName", added.getName());
		assertEquals("tDesc", added.getDescription());
		assertEquals("PasswordResetCode", added.getConsumer());
		assertEquals(subject, added.getMessage().getSubject());
		assertEquals(body, added.getMessage().getBody());
		
		I18nMessage imsg2 = new I18nMessage(new I18nString("stest${code}"), new I18nString("btest${code}"));
		template.setMessage(imsg2);	
		msgTempMan.updateTemplate(template);
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("code", "svalue");
		added = msgTempMan.getTemplate("tName");
		assertEquals("stestsvalue", added.getMessage("pl", "en", params).getSubject());
		assertEquals("btestsvalue", added.getMessage(null, "en", params).getBody());
		
		msgTempMan.removeTemplate("tName");
		assertEquals(2, msgTempMan.listTemplates().size());		
	}
	
	@Test
	public void testValidationConsumer() 
	{
		I18nMessage imsg = new I18nMessage(new I18nString("stest"), new I18nString("btest"));
		MessageTemplate template = new MessageTemplate("tName", "tDesc", imsg, "FailConsumer");
		try
		{
			msgTempMan.addTemplate(template);
		} catch (EngineException e)
		{
			return;
		}
		
		fail("WrongArgumentException not throw.");
	}
	
	@Test
	public void testValidationMessage() 
	{
		I18nMessage imsg = new I18nMessage(new I18nString("stest${code}"), new I18nString("btest"));
		MessageTemplate template = new MessageTemplate("tName", "tDesc", imsg, "RejectForm");
		try
		{
			msgTempMan.addTemplate(template);
		} catch (EngineException e)
		{
			return;
		}
		
		fail("WrongArgumentException not throw.");
	
	}
}
