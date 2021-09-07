/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.registration;

public class UnknownInvitationException extends IllegalArgumentException
{
	public UnknownInvitationException(String message)
	{
		super(message);
	}	
}
