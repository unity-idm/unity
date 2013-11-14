/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap;

/**
 * Exception thrown when the user is not authenticated correctly using the provided credentials.
 * <p>
 * This is used to simply distinguish a common case, which is handled differently from other LDAP connection
 * and usage errors.
 * @author K. Benedyczak
 */
public class LdapAuthenticationException extends Exception
{
	public LdapAuthenticationException(String msg, Exception cause)
	{
		super(msg, cause);
	}
	
	public LdapAuthenticationException(String msg)
	{
		super(msg);
	}
}
