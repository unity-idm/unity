/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.scripts;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import pl.edu.icm.unity.base.event.Event;
import pl.edu.icm.unity.engine.UnityIntegrationTest;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.event.EventCategory;
import pl.edu.icm.unity.engine.api.initializers.ScriptConfiguration;
import pl.edu.icm.unity.engine.api.initializers.ScriptType;

@RunWith(SpringJUnit4ClassRunner.class)
@UnityIntegrationTest
@TestPropertySource(properties = { "unityConfig: src/test/resources/unityServerContentInit.conf" })
public class ContentInitializersExecutorTest
{
	@Autowired
	private UnityServerConfiguration config;
	
	@Mock
	private MainGroovyExecutor groovyMock;
	
	@Before
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
		assertThat(configs.size(), equalTo(1));
		ScriptConfiguration config = configs.get(0);
		assertThat(config.getType(), equalTo(ScriptType.groovy));
		assertThat(config.getTrigger(), equalTo(EventCategory.PRE_INIT.toString()));
		assertThat(config.getFileLocation(), equalTo("classpath:unityServerContentInit.groovy"));
	}
	
	
	@Test
	public void shouldRunGroovyExecutor()
	{
		List<ScriptConfiguration> configs = config.getContentInitializersConfiguration();
		ScriptTriggeringEventListener eventListener = new ScriptTriggeringEventListener(
				config, groovyMock);
		
		Event event = new Event(EventCategory.PRE_INIT);
		eventListener.handleEvent(event);
		
		verify(groovyMock).run(configs.get(0), event);
	}
	
	@Test
	public void shouldNotRunGroovyExecutor()
	{
		ScriptTriggeringEventListener eventListener = new ScriptTriggeringEventListener(
				config, groovyMock);
		
		eventListener.handleEvent(new Event(EventCategory.POST_INIT));
		
		verify(groovyMock, never()).run(any(), any());
	}

}
