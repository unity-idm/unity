/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user;

import io.imunity.scim.exception.SCIMException;

class UserNotFoundException extends SCIMException
{
	UserNotFoundException(final String errorMessage, final Throwable cause)
	{
		super(404, ScimErrorType.invalidValue, errorMessage, cause);
	}

	UserNotFoundException(String errorMessage)
	{
		super(404, ScimErrorType.invalidValue, errorMessage, null);

	}
}
