/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.scripts;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import pl.edu.icm.unity.engine.UnityIntegrationTest;
import pl.edu.icm.unity.engine.api.initializers.ContentInitConf;
import pl.edu.icm.unity.engine.api.initializers.InitializationPhase;
import pl.edu.icm.unity.engine.api.initializers.InitializerType;
import pl.edu.icm.unity.engine.scripts.ContentGroovyExecutor;
import pl.edu.icm.unity.engine.scripts.ContentInitializersExecutor;

@RunWith(SpringJUnit4ClassRunner.class)
@UnityIntegrationTest
@TestPropertySource(properties = { "unityConfig: src/test/resources/unityServerContentInit.conf" })
public class ContentInitializersExecutorTest
{
	@Autowired
	@InjectMocks
	private ContentInitializersExecutor underTest;
	
	@Mock
	private ContentGroovyExecutor groovyMock;
	
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
		List<ContentInitConf> configs = underTest.getContentInitializersConfiguration();
		
		// then
		assertThat(configs.size(), equalTo(1));
		ContentInitConf config = configs.get(0);
		assertThat(config.getType(), equalTo(InitializerType.GROOVY));
		assertThat(config.getPhase(), equalTo(InitializationPhase.PRE_INIT));
		assertThat(config.getFileLocation(), equalTo("unityServerContentInit.groovy"));
	}
	
	
	@Test
	public void shouldRunGroovyExecutor()
	{
		// given
		List<ContentInitConf> configs = underTest.getContentInitializersConfiguration();
		
		// when
		underTest.runPreInitPhase();
		
		// then
		verify(groovyMock).run(configs.get(0));
	}
	
	@Test
	public void shouldNotRunGroovyExecutor()
	{
		// given configuration in unityServerContentInit.conf
		
		// when
		underTest.runPostInitPhase();
		
		// then
		verify(groovyMock, never()).run(any());
	}

}
