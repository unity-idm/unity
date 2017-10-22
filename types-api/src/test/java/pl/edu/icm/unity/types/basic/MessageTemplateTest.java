/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.basic;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.types.I18nMessage;
import pl.edu.icm.unity.types.I18nString;

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

}
