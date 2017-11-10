/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.basic;

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
import pl.edu.icm.unity.types.basic.MessageTemplate.Message;

public class MessageTemplateTest
{
	@Test
	public void shouldRestoreWithPLAINmessageTypeWhenNotPresent()
	{
		// given
		MessageTemplate msg = new MessageTemplate("name", "description", 
				new I18nMessage(new I18nString("test"), new I18nString("test")), 
				"customer", null); 
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
				"customer", null);
		MessageTemplate generic1 = new MessageTemplate("g1", "", 
				new I18nMessage(new I18nString(""), new I18nString("header")), 
				"generic", 
				MessageType.PLAIN);
		MessageTemplate generic2 = new MessageTemplate("g2", "", 
				new I18nMessage(new I18nString(""), new I18nString("footer")), 
				"generic", 
				MessageType.PLAIN);
		Map<String, MessageTemplate> genericTemplates = new HashMap<>();
		genericTemplates.put(generic2.getName(), generic2);
		genericTemplates.put(generic1.getName(), generic1);
		
		// when
		Message message = msg.getMessage(null, null, Collections.emptyMap(), genericTemplates);
		
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
				"customer", null);
		MessageTemplate generic1 = new MessageTemplate("g1", "", 
				new I18nMessage(new I18nString(""), new I18nString("header")), 
				"generic", 
				MessageType.PLAIN);
		MessageTemplate generic2 = new MessageTemplate("g2", "", 
				new I18nMessage(new I18nString(""), new I18nString("footer")), 
				"generic", 
				MessageType.PLAIN);
		
		Map<String, MessageTemplate> genericTemplates = new HashMap<>();
		genericTemplates.put(generic2.getName(), generic2);
		genericTemplates.put(generic1.getName(), generic1);
		
		// when
		Message message = msg.getMessage("pl", "pl", Collections.emptyMap(), genericTemplates);
		
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
				"customer", null);
		I18nString genbody = new I18nString("");
		genbody.addValue("pl", "header");
		MessageTemplate generic1 = new MessageTemplate("g1", "", 
				new I18nMessage(new I18nString(""), genbody), 
				"generic", 
				MessageType.PLAIN);
		MessageTemplate generic2 = new MessageTemplate("g2", "", 
				new I18nMessage(new I18nString(""), new I18nString("footer")), 
				"generic", 
				MessageType.PLAIN);
		
		Map<String, MessageTemplate> genericTemplates = new HashMap<>();
		genericTemplates.put(generic2.getName(), generic2);
		genericTemplates.put(generic1.getName(), generic1);
		
		// when
		Message message = msg.getMessage("pl", "pl", Collections.emptyMap(), genericTemplates);
		
		// then
		assertThat(message.getBody(), equalTo("PREFheaderBODYfooterPOST"));
	}
}
