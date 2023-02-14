/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.secured.shared.endpoint;

import io.imunity.vaadin.endpoint.common.CustomResourceProvider;


public class SecuredSharedResourceProvider extends CustomResourceProvider
{
	public SecuredSharedResourceProvider()
	{
		super("unity-server-vaadin-endpoint-common", "unity-server-vaadin-enquiry");
	}
}
