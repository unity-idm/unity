/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.idp.web;

import io.imunity.vaadin.endpoint.common.CustomResourceProvider;

public class SamlResourceProvider extends CustomResourceProvider
{
	public SamlResourceProvider()
	{
		super("vaadin-endpoint-common", "vaadin-authentication");
	}
}
