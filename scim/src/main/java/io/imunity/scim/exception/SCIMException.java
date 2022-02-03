/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.exception;


public class SCIMException extends RuntimeException
{
	public enum ScimErrorType
	{
		invalidFilter, tooMany, uniqueness, mutability, invalidSyntax, invalidPath, noTarget, invalidValue, invalidVers,
		sensitive
	}

	public final int statusCode;
	public final ScimErrorType scimType;
	public final String errorMessage;

	public SCIMException(int statusCode, final ScimErrorType scimType, final String errorMessage, final Throwable cause)
	{
		super(cause);
		this.scimType = scimType;
		this.statusCode = statusCode;
		this.errorMessage = errorMessage;
	}

}