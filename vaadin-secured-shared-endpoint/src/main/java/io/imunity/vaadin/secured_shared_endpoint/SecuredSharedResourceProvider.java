/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.secured_shared_endpoint;

import io.imunity.vaadin.endpoint.common.CustomResourceProvider;


class SecuredSharedResourceProvider extends CustomResourceProvider
{
	SecuredSharedResourceProvider()
	{
		super("vaadin-endpoint-common", "vaadin-enquiry");
	}
}