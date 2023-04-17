/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.webauthz;

import io.imunity.vaadin.endpoint.common.CustomResourceProvider;

public class OAuthResourceProvider extends CustomResourceProvider
{
	public OAuthResourceProvider()
	{
		super("vaadin-endpoint-common", "vaadin-authentication");
	}
}
