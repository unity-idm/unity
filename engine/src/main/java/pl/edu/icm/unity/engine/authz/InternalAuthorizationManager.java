/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.authz;

import java.util.Set;

import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.AuthorizationExceptionRT;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.Group;


/**
 * Authorizes operations on the engine.
 * @author K. Benedyczak
 */
public interface InternalAuthorizationManager
{
	/**
	 * @return Set of roles supported by the engine
	 */
	Set<String> getRoleNames();

	/**
	 * @return Description of all the roles
	 */
	String getRolesDescription();
	
	/**
	 * As {@link #checkAuthorization(boolean, Group, AuthzCapability...)} with the first argument
	 * false and the second being the root group.
	 * @param group
	 * @param requiredCapabilities
	 * @throws AuthorizationException 
	 */
	void checkAuthorization(AuthzCapability... requiredCapabilities) throws AuthorizationException;

	/**
	 * As {@link #checkAuthorization(boolean, Group, AuthzCapability...)} with the second argument being the root group
	 * @param selfAccess
	 * @param requiredCapabilities
	 * @throws AuthorizationException 
	 */
	void checkAuthorization(boolean selfAccess, AuthzCapability... requiredCapabilities) throws AuthorizationException;
	
	/**
	 * As {@link #checkAuthorization(boolean, Group, AuthzCapability...)} with the first argument
	 * false.
	 * @param group
	 * @param requiredCapabilities
	 * @throws AuthorizationException 
	 */
	void checkAuthorization(String group, AuthzCapability... requiredCapabilities) throws AuthorizationException;

	/**
	 * As {@link #checkAuthorization(Group, AuthzCapability...)} but throws runtime exception
	 */
	void checkAuthorizationRT(String group, AuthzCapability... requiredCapabilities) throws AuthorizationExceptionRT;
	
	/**
	 * Checks the authorization in a specified group. It is checked if the current caller has all the 
	 * requiredCapabilities in the scope of the specified group.
	 * @param selfAccess if this operation is invoked on the the caller itself
	 * @param group
	 * @param requiredCapabilities
	 * @throws AuthorizationException 
	 */
	void checkAuthorization(boolean selfAccess, String group, AuthzCapability... requiredCapabilities) throws AuthorizationException;
	
	/**
	 * Checks authorization to change authorization role attribute in a specific group. 
	 */
	void checkAuthZAttributeChangeAuthorization(boolean selfAccess, Attribute attribute) throws AuthorizationException;
	
	/**
	 * Establishes capabilities of the caller in a specified group.
	 * @param selfAccess if this operation is invoked on the the caller itself
	 * @param group
	 * @throws AuthorizationException thrown only if the caller is not *authenticated*.
	 */
	Set<AuthzCapability> getCapabilities(boolean selfAccess, String group) throws AuthorizationException;
	
	/**
	 * Returns true only if the argument is the same entity as the current caller.
	 * @param subject
	 * @return
	 */
	boolean isSelf(long subject);
	
	/**
	 * Returns the current caller roles.
	 * @throws AuthorizationException thrown only if the caller is not *authenticated*.
	 * @return 
	 */
	Set<AuthzRole> getRoles() throws AuthorizationException;
	
	/**
	 * Empties authZ cache.
	 */
	void clearCache();
}
