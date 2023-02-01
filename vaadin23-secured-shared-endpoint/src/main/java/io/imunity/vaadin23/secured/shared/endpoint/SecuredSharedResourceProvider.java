/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin23.secured.shared.endpoint;

import io.imunity.vaadin23.endpoint.common.CustomResourceProvider;


public class SecuredSharedResourceProvider extends CustomResourceProvider
{
	public SecuredSharedResourceProvider()
	{
		super("vaadin23-endpoint-common", "vaadin23-enquiry");
	}
}
