/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.sessionscope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinSession;

import pl.edu.icm.unity.engine.UnityIntegrationTest;

@ExtendWith(SpringExtension.class)
@UnityIntegrationTest
public class WebSessionScopeTest
{
	@Autowired
	private AbstractApplicationContext ctx;

	@Test
	public void shouldFailToGetObjectWithoutSession()
	{
		Throwable error = catchThrowable(() -> ctx.getBean(WebSessionTestBean.class));
		
		assertThat(error).isInstanceOf(BeanCreationException.class);
	}

	
	@Test
	public void shouldReturnSameInstanceInSession()
	{
		VaadinSession.setCurrent(new MockSession());
		WebSessionTestBean bean1 = ctx.getBean(WebSessionTestBean.class);
		WebSessionTestBean bean2 = ctx.getBean(WebSessionTestBean.class);
		VaadinSession.setCurrent(null);
		
		assertThat(bean2).isEqualTo(bean1);
	}
	
	@Test
	public void shouldReturnDifferentInstanceBetweenSessions()
	{
		VaadinSession.setCurrent(new MockSession());
		WebSessionTestBean bean1 = ctx.getBean(WebSessionTestBean.class);
		VaadinSession.setCurrent(new MockSession());
		WebSessionTestBean bean2 = ctx.getBean(WebSessionTestBean.class);
		VaadinSession.setCurrent(null);
		
		assertThat(bean2).isNotEqualTo(bean1);
	}
	
	
	private static class MockSession extends VaadinSession
	{
		private final Map<String, Object> attributes = new HashMap<>();
		
		MockSession()
		{
			super(null);
		}

		MockSession(VaadinService service)
		{
			super(service);
		}

		@Override
		public <T> T getAttribute(Class<T> type)
		{
			if (type == null)
			{
				throw new IllegalArgumentException("type can not be null");
			}
			Object value = attributes.get(type.getName());
			if (value == null)
			{
				return null;
			} else
			{
				return type.cast(value);
			}
		}

		@Override
		public <T> void setAttribute(Class<T> type, T value)
		{
			if (type == null)
			{
				throw new IllegalArgumentException("type can not be null");
			}
			if (value != null && !type.isInstance(value))
			{
				throw new IllegalArgumentException("value of type " + type.getName()
						+ " expected but got " + value.getClass().getName());
			}
			if (value != null)
				attributes.put(type.getName(), value);
			else
				attributes.remove(type.getName());
		}
	}
}
