/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.front;

import io.imunity.vaadin23.elements.NotificationPresenter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class NotificationConfig
{
	@Bean
	public NotificationPresenter create()
	{
		return new NotificationPresenter();
	}
}
