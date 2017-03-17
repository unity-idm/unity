/*
 * Copyright (c) 2017 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.scripts;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Lists;

import groovy.lang.Binding;
import pl.edu.icm.unity.engine.UnityIntegrationTest;

@RunWith(SpringJUnit4ClassRunner.class)
@UnityIntegrationTest
public class TestMockGroovyProvider
{
	@Autowired
	protected ContentGroovyExecutor groovyExecutor;
	
	@SuppressWarnings({ "rawtypes" })
	@Test
	public void mockAndProdBindingsShouldBeTheSame()
	{
		Binding mockBinding = MockGroovyBindingProvider.getBinding();
		Binding binding = groovyExecutor.getBinding();
		Map variables = binding.getVariables();
		assertThat(mockBinding.getVariables().size(), is(variables.size()));
		for (Object entryO: variables.entrySet())
		{
			Map.Entry entry = (Entry) entryO;
			String key = (String) entry.getKey();
			Object value = entry.getValue();
			Object mockValue = mockBinding.getVariable(key);
			assertThat(mockValue, is(notNullValue()));
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
