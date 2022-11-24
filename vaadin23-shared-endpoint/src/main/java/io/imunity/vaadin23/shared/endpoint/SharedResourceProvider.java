/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin23.shared.endpoint;

import io.imunity.vaadin23.endpoint.common.CustomResourceProvider;
import org.springframework.stereotype.Component;

@Component
public class SharedResourceProvider extends CustomResourceProvider
{
	public SharedResourceProvider()
	{
		super("vaadin23-endpoint-common");
	}
}
