/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.exceptions;

import io.imunity.scim.messages.ErrorResponse;
import io.imunity.scim.messages.ErrorResponse.ScimType;
import pl.edu.icm.unity.exceptions.EngineException;

public class SCIMException extends EngineException
{
	public final ErrorResponse scimError;

	public SCIMException(final ErrorResponse scimError, final Throwable cause)
	{
		super(cause);
		this.scimError = scimError;
	}

	public SCIMException(final int statusCode, final ScimType scimType, final String errorMessage,
			final Throwable cause)
	{
		super(cause);
		this.scimError = ErrorResponse.builder().withDetail(errorMessage).withStatus(statusCode).withScimType(scimType)
				.build();

	}

}