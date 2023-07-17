/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.scripts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import pl.edu.icm.unity.base.event.PersistableEvent;
import pl.edu.icm.unity.engine.UnityIntegrationTest;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.event.EventCategory;
import pl.edu.icm.unity.engine.api.initializers.ScriptConfiguration;
import pl.edu.icm.unity.engine.api.initializers.ScriptType;

@ExtendWith(SpringExtension.class)
@UnityIntegrationTest
@TestPropertySource(properties = { "unityConfig: src/test/resources/unityServerContentInit.conf" })
public class ContentInitializersExecutorTest
{
	@Autowired
	private UnityServerConfiguration config;
	
	@Mock
	private MainGroovyExecutor groovyMock;
	
	@BeforeEach
	public void initializeMock()
	{
		MockitoAnnotations.initMocks(this);
	}
	
	@Test
	public void shouldProperlyParseConfiguration()
	{
		// given unityServerContentInit.conf
		
		// when
		List<ScriptConfiguration> configs = config.getContentInitializersConfiguration();
		
		// then
		assertThat(configs.size()).isEqualTo(1);
		ScriptConfiguration config = configs.get(0);
		assertThat(config.getType()).isEqualTo(ScriptType.groovy);
		assertThat(config.getTrigger()).isEqualTo(EventCategory.PRE_INIT.toString());
		assertThat(config.getFileLocation()).isEqualTo("classpath:unityServerContentInit.groovy");
	}
	
	
	@Test
	public void shouldRunGroovyExecutor()
	{
		List<ScriptConfiguration> configs = config.getContentInitializersConfiguration();
		ScriptTriggeringEventListener eventListener = new ScriptTriggeringEventListener(
				config, groovyMock);
		
		PersistableEvent event = new PersistableEvent(EventCategory.PRE_INIT);
		eventListener.handleEvent(event);
		
		verify(groovyMock).run(configs.get(0), event);
	}
	
	@Test
	public void shouldNotRunGroovyExecutor()
	{
		ScriptTriggeringEventListener eventListener = new ScriptTriggeringEventListener(
				config, groovyMock);
		
		eventListener.handleEvent(new PersistableEvent(EventCategory.POST_INIT));
		
		verify(groovyMock, never()).run(any(), any());
	}

}
