/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.msg;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.Message;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.store.api.MessagesDAO;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations={"classpath*:META-INF/components.xml"})
@TestPropertySource(properties = { "unityConfig: src/test/resources/unityServer.conf" })
public class TestUnityMessageSource
{
	@Autowired
	private MessageSource msg;
	
	@Autowired
	private MessageRepository repository;
	
	@Autowired
	private MessagesDAO dao;
	
	@Autowired
	private TransactionalRunner tx;
	
	@Test
	public void messageFromExternalModuleIsResolved()
	{
		String message = msg.getMessage("MessageTemplateConsumer.EnquiryFilled.var.user");
		
		assertThat(message).isEqualTo("Identity of the user");
	}

	@Test
	public void messageFromCurrentModuleIsResolved()
	{
		String message = msg.getMessage("MessageUsedForIntegrationTesting.only0");
		
		assertThat(message).isEqualTo("From default bundle");
	}

	@Test
	public void messageFromFileIsResolved()
	{
		String message = msg.getMessage("MessageUsedForIntegrationTesting.only2");
		
		assertThat(message).isEqualTo("From extra file");
	}

	@Test
	public void messageFromFileOverrideBundled()
	{
		String message = msg.getMessage("MessageUsedForIntegrationTesting.only1");
		
		assertThat(message).isEqualTo("From extra file");
	}

	@Test
	public void messageFromDBIsLoaded() throws EngineException
	{
		tx.runInTransaction(() -> {
			dao.deleteAll();
			dao.create(new Message("test.test", Locale.ENGLISH, "test val"));
			repository.reload();
		});
		String message = msg.getMessage("test.test", Locale.ENGLISH);
		assertThat(message).isEqualTo("test val");
	}
	
	@Test
	public void i18nmessageIsMerged() throws EngineException
	{
		tx.runInTransaction(() -> {
			dao.deleteAll();
			dao.create(new Message("MessageUsedForIntegrationTesting.only1", Locale.GERMAN, "test val"));
			repository.reload();
		});
		I18nString message = msg.getI18nMessage("MessageUsedForIntegrationTesting.only1");
		assertThat(message.getMap().size()).isEqualTo(2);
		assertThat(message.getMap().values()).contains("From extra file", "test val");
	}
}
