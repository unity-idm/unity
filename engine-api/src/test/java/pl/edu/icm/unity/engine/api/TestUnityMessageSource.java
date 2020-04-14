/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath*:META-INF/components.xml"})
@TestPropertySource(properties = { "unityConfig: src/test/resources/unityServer.conf" })
public class TestUnityMessageSource
{
	@Autowired
	private UnityMessageSource msg;
	
	@Test
	public void messageFromExternalModuleIsResolved()
	{
		String message = msg.getMessage("MessageTemplateConsumer.EnquiryFilled.var.user");
		
		assertThat(message, is("Identity of the user"));
	}

	@Test
	public void messageFromCurrentModuleIsResolved()
	{
		String message = msg.getMessage("MessageUsedForIntegrationTesting.only0");
		
		assertThat(message, is("From default bundle"));
	}

	@Test
	public void messageFromFileIsResolved()
	{
		String message = msg.getMessage("MessageUsedForIntegrationTesting.only2");
		
		assertThat(message, is("From extra file"));
	}

	@Test
	public void messageFromFileOverrideBundled()
	{
		String message = msg.getMessage("MessageUsedForIntegrationTesting.only1");
		
		assertThat(message, is("From extra file"));
	}
	
	@Test
	public void test()
	{
		
		msg.getKeys().keySet().forEach(k -> System.out.println(k));
	}
}
