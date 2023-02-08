/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.shared.endpoint;

import io.imunity.vaadin.endpoint.common.CustomResourceProvider;
import org.springframework.stereotype.Component;

@Component
public class SharedResourceProvider extends CustomResourceProvider
{
	public SharedResourceProvider()
	{
		super("vaadin-endpoint-common", "vaadin-enquiry", "vaadin-registration");
	}
}
