/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.schema.resourceType;

import io.imunity.scim.exception.SCIMException;

class ResourceTypeNotFoundException extends SCIMException
{
	ResourceTypeNotFoundException(final String errorMessage, final Throwable cause)
	{
		super(404, ScimErrorType.invalidValue, errorMessage, cause);
	}

	ResourceTypeNotFoundException(String errorMessage)
	{
		super(404, ScimErrorType.invalidValue, errorMessage, null);
	}
}
