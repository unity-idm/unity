/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.handlers;

import io.imunity.scim.config.SCIMEndpointDescription;

public interface SCIMHandlerFactory
{
	SCIMHandler getHandler(SCIMEndpointDescription configuration);
}
