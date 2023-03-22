/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.idp.web;

import io.imunity.vaadin.endpoint.common.CustomResourceProvider;
import org.springframework.stereotype.Component;

@Component
class SamlResourceProvider extends CustomResourceProvider
{
	SamlResourceProvider()
	{
		super("vaadin-endpoint-common");
	}
}
