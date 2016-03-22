/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.endpoint;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.server.api.internal.IdentityResolver;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;

/**
 * Responsible for mapping LDAP user identities to Unity entities.
 * 
 * @author K. Benedyczak
 */
@Component
public class UserMapper
{
	public static final String[] IDENTITY_TYPES = {UsernameIdentity.ID, EmailIdentity.ID};
	
	@Autowired
	private IdentityResolver identityResolver;

	/**
	 * @param username
	 * @return entity id of the given user
	 * @throws IllegalIdentityValueException
	 */
	public long resolveUser(String username, String realm) throws IllegalIdentityValueException 
	{
		try
		{
			return identityResolver.resolveIdentity(username, IDENTITY_TYPES, null,  realm);
		} catch (IllegalIdentityValueException e)
		{
			throw e;
		} catch (Exception e)
		{
			throw new IllegalIdentityValueException("Invalid user", e);
		}
	}
}
