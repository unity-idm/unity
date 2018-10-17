/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.msgtemplate;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.types.I18nMessage;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.MessageTemplate;
import pl.edu.icm.unity.types.basic.MessageType;
import pl.edu.icm.unity.types.basic.MessageTemplate.Message;

public class MessageTemplateProcessorTest
{
	@Test
	public void shouldRestoreWithPLAINmessageTypeWhenNotPresent()
	{
		// given
		MessageTemplate msg = new MessageTemplate("name", "description", 
				new I18nMessage(new I18nString("test"), new I18nString("test")), 
				"customer", null, "channel"); 
		String jsonString = JsonUtil.toJsonString(msg);
		ObjectNode objNode = JsonUtil.parse(jsonString);
		objNode.remove("type");
		
		// when
		MessageTemplate restore = new MessageTemplate(objNode);
		
		// then
		assertThat(restore.getType(), equalTo(MessageType.PLAIN));
	}

	
	@Test
	public void shouldResolveTwoIncludesInDefaultBody()
	{
		// given
		MessageTemplate msg = new MessageTemplate("name", "description", 
				new I18nMessage(new I18nString("test"), 
						new I18nString("PREF${include:g1}BODY${include:g2}POST")), 
				"customer", null, "channel");
		MessageTemplate generic1 = new MessageTemplate("g1", "", 
				new I18nMessage(new I18nString(""), new I18nString("header")), 
				"generic", 
				MessageType.PLAIN, "channel");
		MessageTemplate generic2 = new MessageTemplate("g2", "", 
				new I18nMessage(new I18nString(""), new I18nString("footer")), 
				"generic", 
				MessageType.PLAIN, "channel");
		Map<String, MessageTemplate> genericTemplates = new HashMap<>();
		genericTemplates.put(generic2.getName(), generic2);
		genericTemplates.put(generic1.getName(), generic1);
		
		// when
		Message message = new MessageTemplateProcessor().getMessage(msg, null, null, 
				Collections.emptyMap(), genericTemplates);
		
		// then
		assertThat(message.getBody(), equalTo("PREFheaderBODYfooterPOST"));
	}
	
	@Test
	public void shouldResolveTwoIncludesInLocaleBodyFromDefaultLocale()
	{
		// given
		I18nString body = new I18nString("");
		body.addValue("pl", "PREF${include:g1}BODY${include:g2}POST");
		MessageTemplate msg = new MessageTemplate("name", "description", 
				new I18nMessage(new I18nString("test"), body), 
				"customer", null, "channel"); 
		MessageTemplate generic1 = new MessageTemplate("g1", "", 
				new I18nMessage(new I18nString(""), new I18nString("header")), 
				"generic", 
				MessageType.PLAIN, "channel"); 
		MessageTemplate generic2 = new MessageTemplate("g2", "", 
				new I18nMessage(new I18nString(""), new I18nString("footer")), 
				"generic", 
				MessageType.PLAIN, "channel"); 
		
		Map<String, MessageTemplate> genericTemplates = new HashMap<>();
		genericTemplates.put(generic2.getName(), generic2);
		genericTemplates.put(generic1.getName(), generic1);
		
		// when
		Message message = new MessageTemplateProcessor().getMessage(msg, "pl", "pl", 
				Collections.emptyMap(), genericTemplates);
		
		// then
		assertThat(message.getBody(), equalTo("PREFheaderBODYfooterPOST"));
	}
	
	@Test
	public void shouldResolveTwoIncludesInLocaleBodyFromLocaleInclude()
	{
		// given
		I18nString body = new I18nString("");
		body.addValue("pl", "PREF${include:g1}BODY${include:g2}POST");
		MessageTemplate msg = new MessageTemplate("name", "description", 
				new I18nMessage(new I18nString("test"), body), 
				"customer", null, "channel");
		I18nString genbody = new I18nString("");
		genbody.addValue("pl", "header");
		MessageTemplate generic1 = new MessageTemplate("g1", "", 
				new I18nMessage(new I18nString(""), genbody), 
				"generic", 
				MessageType.PLAIN, "channel"); 
		MessageTemplate generic2 = new MessageTemplate("g2", "", 
				new I18nMessage(new I18nString(""), new I18nString("footer")), 
				"generic", 
				MessageType.PLAIN, "channel"); 
		
		Map<String, MessageTemplate> genericTemplates = new HashMap<>();
		genericTemplates.put(generic2.getName(), generic2);
		genericTemplates.put(generic1.getName(), generic1);
		
		// when
		Message message = new MessageTemplateProcessor().getMessage(msg, "pl", "pl", 
				Collections.emptyMap(), genericTemplates);
		
		// then
		assertThat(message.getBody(), equalTo("PREFheaderBODYfooterPOST"));
	}
}
