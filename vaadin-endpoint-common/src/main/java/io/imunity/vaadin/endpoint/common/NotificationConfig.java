/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common;

import io.imunity.vaadin.elements.NotificationPresenter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
class NotificationConfig
{
	@Bean
	@Primary
	public NotificationPresenter create()
	{
		return new NotificationPresenter();
	}
}
