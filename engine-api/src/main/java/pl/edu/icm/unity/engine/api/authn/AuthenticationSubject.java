/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn;

/**
 * DTO containing information about a user being authenticated. In case of first factor it is identity of unknown type.
 *  In case of 2nd factor it is a concrete entity.
 */
public class AuthenticationSubject
{
	public final String identity;
	public final Long entityId;
	
	private AuthenticationSubject(String identity, Long entityId)
	{
		this.identity = identity;
		this.entityId = entityId;
	}

	public static AuthenticationSubject identityBased(String identity)
	{
		return new AuthenticationSubject(identity, null);
	}

	public static AuthenticationSubject entityBased(long entityId)
	{
		return new AuthenticationSubject(null, entityId);
	}

	@Override
	public String toString()
	{
		return identity == null ? String.valueOf(entityId) : identity;
	}
}
