/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.idp.ecp;

import io.imunity.vaadin.elements.NotificationPresenter;
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
