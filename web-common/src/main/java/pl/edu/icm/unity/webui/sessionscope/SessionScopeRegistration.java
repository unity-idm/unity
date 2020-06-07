/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.sessionscope;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class SessionScopeRegistration
{
	@Bean
	public static BeanFactoryPostProcessor beanFactoryPostProcessor()
	{
		return new SessionBeanFactoryPostprocessor();
	}
	
	static class SessionBeanFactoryPostprocessor implements BeanFactoryPostProcessor
	{
		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException
		{
			beanFactory.registerScope(WebSessionScope.NAME, new WebSessionScope());
		}
	}
}
