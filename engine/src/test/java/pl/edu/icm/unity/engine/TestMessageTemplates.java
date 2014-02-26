package pl.edu.icm.unity.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.msgtemplates.MessageTemplate;
import pl.edu.icm.unity.msgtemplates.MessageTemplate.Message;
import pl.edu.icm.unity.server.api.MessageTemplateManagement;

public class TestMessageTemplates extends DBIntegrationTestBase
{
	@Autowired
	MessageTemplateManagement msgTempMan;
	
	@Test
	public void testPersistence() throws Exception
	{
		assertEquals(1, msgTempMan.listTemplates().size());
		Map<String, Message> imsg = new HashMap<String, Message>();
		imsg.put("", new Message("stest", "btest"));
		MessageTemplate template = new MessageTemplate("tName", "tDesc", imsg, "RejectForm");
		msgTempMan.addTemplate(template);
		assertEquals(2, msgTempMan.listTemplates().size());
		MessageTemplate added = msgTempMan.getTemplate("tName");
		assertEquals("tName", added.getName());
		assertEquals("tDesc", added.getDescription());
		assertEquals("RejectForm", added.getConsumer());
		assertEquals(1, added.getAllMessages().size());
		assertEquals("stest", added.getRawMessage().getSubject());
		assertEquals("btest", added.getRawMessage().getBody());
		
		
		Map<String, Message> imsg2 = new HashMap<String, Message>();
		imsg2.put("", new Message("stest${sub}", "btest${body}"));
		template.setAllMessages(imsg2);	
		msgTempMan.updateTemplate(template);
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("sub", "svalue");
		params.put("body", "bvalue");
		added = msgTempMan.getTemplate("tName");
		assertEquals("stestsvalue", added.getMessage(params).getSubject());
		assertEquals("btestbvalue", added.getMessage(params).getBody());
		
		msgTempMan.removeTemplate("tName");
		assertEquals(1, msgTempMan.listTemplates().size());		
	}
	
	@Test
	public void testValidation() 
	{
		Map<String, Message> imsg = new HashMap<String, Message>();
		imsg.put("", new Message("stest", "btest"));
		MessageTemplate template = new MessageTemplate("tName", "tDesc", imsg, "FailConsumer");
		try
		{
			msgTempMan.addTemplate(template);
		} catch (EngineException e)
		{
			return;
		}
		fail("InvalidArgumentException not throw.");
	}
}
