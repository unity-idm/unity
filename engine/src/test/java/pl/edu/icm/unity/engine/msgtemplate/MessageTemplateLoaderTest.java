/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.msgtemplate;

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.i18n.I18nMessage;
import pl.edu.icm.unity.base.msg_template.MessageTemplate;
import pl.edu.icm.unity.base.notifications.NotificationChannelInfo;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.NotificationsManagement;

public class MessageTemplateLoaderTest
{
	@Test
	public void shouldLoadWithDefaultLocale() throws EngineException
	{
		MessageTemplateManagement man = mock(MessageTemplateManagement.class);
		when(man.listTemplates()).thenReturn(Collections.emptyMap());
		ArgumentCaptor<MessageTemplate> captor = ArgumentCaptor
				.forClass(MessageTemplate.class);
		MessageTemplateLoader loader = new MessageTemplateLoader(man,
				getMockNotificationManager(), false);
		
		Properties props = new Properties();
		props.setProperty("msg1.subject", "sub");
		props.setProperty("msg1.body", "body");
		loader.initializeMsgTemplates(props, s->true);
		
		verify(man).addTemplate(captor.capture());
		
		I18nMessage msg = captor.getValue().getMessage();
		assertThat(msg.getSubject().getDefaultValue()).isEqualTo("sub");
		assertThat(msg.getSubject().getMap().size()).isEqualTo(0);
		assertThat(msg.getBody().getDefaultValue()).isEqualTo("body");
		assertThat(msg.getBody().getMap().size()).isEqualTo(0);
	}

	@Test
	public void shouldLoadWithNonDefaultLocale() throws EngineException
	{
		MessageTemplateManagement man = mock(MessageTemplateManagement.class);
		when(man.listTemplates()).thenReturn(Collections.emptyMap());
		ArgumentCaptor<MessageTemplate> captor = ArgumentCaptor
				.forClass(MessageTemplate.class);
		MessageTemplateLoader loader = new MessageTemplateLoader(man,
				getMockNotificationManager(), false);
		
		Properties props = new Properties();
		props.setProperty("msg1.subject.en", "sub");
		props.setProperty("msg1.body.en", "body");
		loader.initializeMsgTemplates(props, s->true);
		
		verify(man).addTemplate(captor.capture());
		
		I18nMessage msg = captor.getValue().getMessage();
		assertThat(msg.getSubject().getDefaultValue()).isNull();
		assertThat(msg.getBody().getDefaultValue()).isNull();
		assertThat(msg.getSubject().getValueRaw("en")).isEqualTo("sub");
		assertThat(msg.getBody().getValueRaw("en")).isEqualTo("body");
		assertThat(msg.getSubject().getMap().size()).isEqualTo(1);
		assertThat(msg.getBody().getMap().size()).isEqualTo(1);
	}

	@Test
	public void shouldLoadTwoLocalesAndDef() throws EngineException
	{
		MessageTemplateManagement man = mock(MessageTemplateManagement.class);
		when(man.listTemplates()).thenReturn(Collections.emptyMap());
		ArgumentCaptor<MessageTemplate> captor = ArgumentCaptor
				.forClass(MessageTemplate.class);
		MessageTemplateLoader loader = new MessageTemplateLoader(man,
				getMockNotificationManager(), false);
		
		Properties props = new Properties();
		props.setProperty("msg1.subject", "sub");
		props.setProperty("msg1.body.pl", "body-pl");
		props.setProperty("msg1.body.en", "body-en");
		props.setProperty("msg1.body", "body");
		loader.initializeMsgTemplates(props, s->true);
		
		verify(man).addTemplate(captor.capture());
		
		I18nMessage msg = captor.getValue().getMessage();
		assertThat(msg.getSubject().getDefaultValue()).isEqualTo("sub");
		assertThat(msg.getBody().getDefaultValue()).isEqualTo("body");
		assertThat(msg.getBody().getValueRaw("pl")).isEqualTo("body-pl");
		assertThat(msg.getBody().getValueRaw("en")).isEqualTo("body-en");
		assertThat(msg.getSubject().getMap().size()).isEqualTo(0);
		assertThat(msg.getBody().getMap().size()).isEqualTo(2);
	}

	@Test
	public void shouldLoadOverrwriteInlineBodyWithFileBody() throws EngineException
	{
		MessageTemplateManagement man = mock(MessageTemplateManagement.class);
		when(man.listTemplates()).thenReturn(Collections.emptyMap());
		ArgumentCaptor<MessageTemplate> captor = ArgumentCaptor
				.forClass(MessageTemplate.class);
		MessageTemplateLoader loader = new MessageTemplateLoader(man,
				getMockNotificationManager(), false);
		
		Properties props = new Properties();
		props.setProperty("msg1.subject", "sub");
		props.setProperty("msg1.body.en", "body-inline");
		props.setProperty("msg1.bodyFile.en", "src/test/resources/templateBody.txt");
		loader.initializeMsgTemplates(props, s->true);
		
		verify(man).addTemplate(captor.capture());
		
		I18nMessage msg = captor.getValue().getMessage();
		assertThat(msg.getSubject().getDefaultValue()).isEqualTo("sub");
		assertThat(msg.getBody().getDefaultValue()).isNull();
		assertThat(msg.getBody().getValueRaw("en")).isEqualTo("FILE");
		assertThat(msg.getSubject().getMap().size()).isEqualTo(0);
		assertThat(msg.getBody().getMap()).hasSize(1);

	}
	
	private NotificationsManagement getMockNotificationManager() throws EngineException
	{
		NotificationsManagement notMan = mock(NotificationsManagement.class);
		Map<String, NotificationChannelInfo> res = new HashMap<>();
		res.put("", null);	
		when(notMan.getNotificationChannels()).thenReturn(res);
		return notMan;
	}
}
