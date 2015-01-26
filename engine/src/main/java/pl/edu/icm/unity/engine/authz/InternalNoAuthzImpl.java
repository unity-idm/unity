/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.authz;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.AuthorizationException;


/**
 * Empty authorization module - everybody is authorized. Used by insecure management beans,
 * which are used internally, mostly to set up the initial server state.
 * @author K. Benedyczak
 */
@Component
@Qualifier("insecure")
public class InternalNoAuthzImpl implements AuthorizationManager
{
	@Override
	public Set<String> getRoleNames()
	{
		return Collections.emptySet();
	}

	@Override
	public void checkAuthorization(AuthzCapability... requiredCapabilities)
	{
	}

	@Override
	public void checkAuthorization(boolean selfAccess, AuthzCapability... requiredCapabilities)
	{
	}
	
	@Override
	public void checkAuthorization(String group, AuthzCapability... requiredCapabilities)
	{
	}

	@Override
	public void checkAuthorization(boolean selfAccess, String group, AuthzCapability... requiredCapabilities)
	{
	}
	
	@Override
	public boolean isSelf(long subject)
	{
		return false;
	}

	@Override
	public String getRolesDescription()
	{
		return "";
	}

	@Override
	public Set<AuthzCapability> getCapabilities(boolean selfAccess, String group)
			throws AuthorizationException
	{
		Set<AuthzCapability> ret = new HashSet<AuthzCapability>();
		Collections.addAll(ret, AuthzCapability.values());
		return ret;
	}
}
