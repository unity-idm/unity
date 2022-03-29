/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.exception.providers;

import java.util.Set;

public class SCIMEndpointExceptionMapper
{
	public static void installExceptionHandlers(Set<Object> ret)
	{
		ret.add(new EngineExceptionMapper());
		ret.add(new UnknownIdentityExceptionMapper());
		ret.add(new UnknownGroupExceptionMapper());
		ret.add(new SCIMExceptionMapper());
	}
}
