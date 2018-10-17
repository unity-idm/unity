/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.scripts;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Lists;

import groovy.lang.Binding;
import pl.edu.icm.unity.base.event.Event;
import pl.edu.icm.unity.engine.UnityIntegrationTest;
import pl.edu.icm.unity.engine.api.event.EventCategory;

@RunWith(SpringJUnit4ClassRunner.class)
@UnityIntegrationTest
public class TestMockGroovyProvider
{
	@Autowired
	protected MainGroovyExecutor groovyExecutor;
	
	@SuppressWarnings({ "rawtypes" })
	@Test
	public void mockAndProdBindingsShouldBeTheSame()
	{
		Event event = new Event(EventCategory.POST_INIT.name(), -1l, new Date(), ""); 
		Binding mockBinding = MockGroovyBindingProvider.getBinding(event);
		Binding binding = groovyExecutor.getBinding(event);
		Map variables = binding.getVariables();
		assertThat(mockBinding.getVariables().size(), is(variables.size()));
		for (Object entryO: variables.entrySet())
		{
			Map.Entry entry = (Entry) entryO;
			String key = (String) entry.getKey();
			Object value = entry.getValue();
			Object mockValue = mockBinding.getVariable(key);
			assertThat(key, mockValue, is(notNullValue()));
			for (Class<?> implementedIface: value.getClass().getInterfaces())
			{
				if (!implementedIface.getPackage().getName().startsWith("pl.edu.icm"))
					continue;
				assertThat(Lists.newArrayList(value.getClass().getInterfaces()), 
						hasItem(implementedIface));
			}
		}
	}
}
