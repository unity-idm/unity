/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.group;

import io.imunity.scim.exception.SCIMException;

class GroupNotFoundException extends SCIMException
{

	GroupNotFoundException(final String errorMessage, final Throwable cause)
	{
		super(404, null, errorMessage, cause);
	}

	GroupNotFoundException(String errorMessage)
	{
		super(404, ScimErrorType.invalidValue, errorMessage, null);

	}
}
