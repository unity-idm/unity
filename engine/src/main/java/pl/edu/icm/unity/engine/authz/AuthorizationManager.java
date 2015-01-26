/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.authz;

import java.util.Set;

import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.types.basic.Group;


/**
 * Authorizes operations on the engine.
 * @author K. Benedyczak
 */
public interface AuthorizationManager
{
	/**
	 * @return Set of roles supported by the engine
	 */
	public Set<String> getRoleNames();

	/**
	 * @return Description of all the roles
	 */
	public String getRolesDescription();
	
	/**
	 * As {@link #checkAuthorization(boolean, Group, AuthzCapability...)} with the first argument
	 * false and the second being the root group.
	 * @param group
	 * @param requiredCapabilities
	 * @throws AuthorizationException 
	 */
	public void checkAuthorization(AuthzCapability... requiredCapabilities) throws AuthorizationException;

	/**
	 * As {@link #checkAuthorization(boolean, Group, AuthzCapability...)} with the second argument being the root group
	 * @param selfAccess
	 * @param requiredCapabilities
	 * @throws AuthorizationException 
	 */
	public void checkAuthorization(boolean selfAccess, AuthzCapability... requiredCapabilities) throws AuthorizationException;
	
	/**
	 * As {@link #checkAuthorization(boolean, Group, AuthzCapability...)} with the first argument
	 * false.
	 * @param group
	 * @param requiredCapabilities
	 * @throws AuthorizationException 
	 */
	public void checkAuthorization(String group, AuthzCapability... requiredCapabilities) throws AuthorizationException;

	/**
	 * Checks the authorization in a specified group. It is checked if the current caller has all the 
	 * requiredCapabilities in the scope of the specified group.
	 * @param selfAccess if this operation is invoked on the the caller itself
	 * @param group
	 * @param requiredCapabilities
	 * @throws AuthorizationException 
	 */
	public void checkAuthorization(boolean selfAccess, String group, AuthzCapability... requiredCapabilities) throws AuthorizationException;
	
	/**
	 * Establishes capabilities of the caller in a specified group.
	 * @param selfAccess if this operation is invoked on the the caller itself
	 * @param group
	 * @throws AuthorizationException thrown only if the caller is not *authenticated*.
	 */
	public Set<AuthzCapability> getCapabilities(boolean selfAccess, String group) throws AuthorizationException;
	
	/**
	 * Returns true only if the argument is the same entity as the current caller.
	 * @param subject
	 * @return
	 */
	public boolean isSelf(long subject);
}
