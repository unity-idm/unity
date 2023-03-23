/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.token.access;

import io.imunity.vaadin.elements.NotificationPresenter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class Config
{
	@Bean
	NotificationPresenter notificationPresenter()
	{
		return new NotificationPresenter();
	}
}
