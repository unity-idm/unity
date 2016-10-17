/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.client;

import com.unboundid.ldap.sdk.LDAPException;

@FunctionalInterface
public interface LdapExecFunctor<T, U, R> {
    R apply(T t, U u) throws LDAPException, LdapAuthenticationException;
}