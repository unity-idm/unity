/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.scripts;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.Lists;

import groovy.lang.Binding;
import pl.edu.icm.unity.base.event.PersistableEvent;
import pl.edu.icm.unity.engine.UnityIntegrationTest;
import pl.edu.icm.unity.engine.api.event.EventCategory;

@ExtendWith(SpringExtension.class)
@UnityIntegrationTest
public class TestMockGroovyProvider
{
	@Autowired
	protected MainGroovyExecutor groovyExecutor;
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void mockAndProdBindingsShouldBeTheSame()
	{
		PersistableEvent event = new PersistableEvent(EventCategory.POST_INIT.name(), -1l, new Date(), "");
		Binding mockBinding = MockGroovyBindingProvider.getBinding(event);
		Binding binding = groovyExecutor.getBinding(event);
		Map variables = binding.getVariables();
		assertThat(mockBinding.getVariables()).hasSize(variables.size());
		for (Object entryO: variables.entrySet())
		{
			Map.Entry entry = (Entry) entryO;
			String key = (String) entry.getKey();
			Object value = entry.getValue();
			Object mockValue = mockBinding.getVariable(key);
			assertThat(mockValue).isNotNull();
			for (Class<?> implementedIface: value.getClass().getInterfaces())
			{
				if (!implementedIface.getPackage().getName().startsWith("pl.edu.icm"))
					continue;
				assertThat(Lists.newArrayList(value.getClass().getInterfaces())). 
						contains(implementedIface);
			}
		}
	}
}
