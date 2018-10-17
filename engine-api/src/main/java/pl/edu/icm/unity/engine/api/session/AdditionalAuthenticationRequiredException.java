/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.session;

/**
 * Signals that additional authentication is required prior to invoking the operation
 * 
 * @author K. Benedyczak
 */
public class AdditionalAuthenticationRequiredException extends RuntimeException
{
	public final String authenticationOption;

	public AdditionalAuthenticationRequiredException(String authenticationOption)
	{
		this.authenticationOption = authenticationOption;
	}
	
	@Override
	public String getMessage()
	{
		return "Additional authentication with " + authenticationOption + " is required";
	}
}