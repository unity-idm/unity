/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.exceptions;

import io.imunity.scim.messages.ErrorResponse;
import io.imunity.scim.messages.ErrorResponse.ScimType;

public class ResourceNotFoundException extends SCIMException
{

	public ResourceNotFoundException(final String errorMessage, final Throwable cause)
	{
		super(404, null, errorMessage, cause);
	}

	public ResourceNotFoundException(ErrorResponse scimError, Throwable cause)
	{
		super(scimError, cause);

	}

	public ResourceNotFoundException(String errorMessage)
	{
		super(404, ScimType.invalidValue, errorMessage, null);

	}

}
