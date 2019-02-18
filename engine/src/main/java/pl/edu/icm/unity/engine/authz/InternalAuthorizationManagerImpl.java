/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.authz;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.AuthorizationExceptionRT;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.Group;


/**
 * Default implementation of the {@link InternalAuthorizationManager}
 * @author K. Benedyczak
 */
@Component
@Primary
public class InternalAuthorizationManagerImpl implements InternalAuthorizationManager
{
	/**
	 * System manager role with all privileges. Must not be removed or modified.
	 */
	public static final String SYSTEM_MANAGER_ROLE = "System Manager";

	public static final String CONTENTS_MANAGER_ROLE = "Contents Manager";  
	public static final String PRIVILEGED_INSPECTOR_ROLE = "Privileged Inspector";
	public static final String INSPECTOR_ROLE = "Inspector";
	public static final String USER_ROLE = "Regular User";
	public static final String ANONYMOUS_ROLE = "Anonymous User";

	private Map<String, AuthzRole> roles = new LinkedHashMap<>(); 

	private CachingRolesResolver rolesResolver;
			
	@Autowired
	public InternalAuthorizationManagerImpl(AttributesHelper dbAttributes, UnityServerConfiguration config, 
			GroupDAO groupDAO)
	{
		setupRoleCapabilities();
		rolesResolver = new CachingRolesResolver(roles, dbAttributes, 
				config.getLongValue(UnityServerConfiguration.AUTHZ_CACHE_MS), groupDAO);
	}
	
	/**
	 * Initialization: what capabilities are assigned to roles. In future this might be updatable.
	 */
	private void setupRoleCapabilities()
	{
		setupRole(new RoleImpl(SYSTEM_MANAGER_ROLE, "System manager with all privileges.", 
				new AuthzCapability[] {
					AuthzCapability.maintenance,
					AuthzCapability.attributeModify, 
					AuthzCapability.groupModify,
					AuthzCapability.identityModify,
					AuthzCapability.credentialModify,
					AuthzCapability.readHidden,
					AuthzCapability.read,
					AuthzCapability.readInfo
				}));

		setupRole(new RoleImpl(CONTENTS_MANAGER_ROLE, "Allows for performing all management operations related" +
				" to groups, entities and attributes. Also allows for reading information about " +
				"hidden attributes.", 
				new AuthzCapability[] {
					AuthzCapability.attributeModify, 
					AuthzCapability.groupModify,
					AuthzCapability.identityModify,
					AuthzCapability.credentialModify,
					AuthzCapability.readHidden,
					AuthzCapability.read,
					AuthzCapability.readInfo
				}));

		setupRole(new RoleImpl(PRIVILEGED_INSPECTOR_ROLE, "Allows for reading entities, groups and attributes,"
				+ " including the attributes visible locally only. " +
				"No modifications are possible", 
				new AuthzCapability[] {
					AuthzCapability.readHidden,
					AuthzCapability.read,
					AuthzCapability.readInfo
				},
				new AuthzCapability[] {
					AuthzCapability.credentialModify,
					AuthzCapability.attributeModify,
					AuthzCapability.identityModify,
					AuthzCapability.read
				}));
		
		setupRole(new RoleImpl(INSPECTOR_ROLE, "Allows for reading entities, groups and attributes. " +
				"No modifications are possible", 
				new AuthzCapability[] {
					AuthzCapability.read,
					AuthzCapability.readInfo
				},
				new AuthzCapability[] {
					AuthzCapability.credentialModify,
					AuthzCapability.attributeModify,
					AuthzCapability.identityModify,
					AuthzCapability.read
				}));
		
		setupRole(new RoleImpl(USER_ROLE, "Allows owners for reading of the basic system information," +
				" retrieval of information about themselves and also for changing " +
				"self managed attributes, identities and passwords", 
				new AuthzCapability[] {
					AuthzCapability.readInfo
				},
				new AuthzCapability[] {
					AuthzCapability.credentialModify,
					AuthzCapability.attributeModify,
					AuthzCapability.identityModify,
					AuthzCapability.read
				}));

		setupRole(new RoleImpl(ANONYMOUS_ROLE, "Allows for minimal access to the system: " +
				"owners can get basic system information and retrieve information about themselves", 
				new AuthzCapability[] {
					AuthzCapability.readInfo
				}, 
				new AuthzCapability[] {
					AuthzCapability.read
				}));
	}

	private void setupRole(AuthzRole role)
	{
		roles.put(role.getName(), role);
	}
	
	@Override
	public Set<String> getRoleNames()
	{
		return roles.keySet();
	}

	@Override
	public String getRolesDescription()
	{
		StringBuilder ret = new StringBuilder();
		for (AuthzRole role: roles.values())
		{
			ret.append("<b>").append(role.getName()).append("</b> - ").
				append(role.getDescription()).append("\n");
		}
		return ret.toString();
	}
	
	@Override
	@Transactional
	public void checkAuthorization(AuthzCapability... requiredCapabilities) throws AuthorizationException
	{
		checkAuthorizationInternal(getCallerMethodName(2), false, null, requiredCapabilities);
	}

	@Override
	@Transactional
	public void checkAuthorization(boolean selfAccess, AuthzCapability... requiredCapabilities) throws AuthorizationException
	{
		checkAuthorizationInternal(getCallerMethodName(2), selfAccess, null, requiredCapabilities);
	}
	
	@Override
	@Transactional
	public void checkAuthorization(String group, AuthzCapability... requiredCapabilities) throws AuthorizationException
	{
		checkAuthorizationInternal(getCallerMethodName(2), false, group, requiredCapabilities);
	}

	@Override
	@Transactional
	public void checkAuthorizationRT(String group, AuthzCapability... requiredCapabilities)
			throws AuthorizationExceptionRT
	{
		try
		{
			checkAuthorizationInternal(getCallerMethodName(2), false, group, requiredCapabilities);
		} catch (AuthorizationException e)
		{
			throw new AuthorizationExceptionRT(e.getMessage(), e.getCause());
		}
	}

	@Override
	@Transactional
	public void checkAuthorization(boolean selfAccess, String groupPath, AuthzCapability... requiredCapabilities) throws AuthorizationException
	{
		checkAuthorizationInternal(getCallerMethodName(2), selfAccess, groupPath, requiredCapabilities);
	}

	@Override
	@Transactional
	public void checkAuthZAttributeChangeAuthorization(boolean selfAccess, Attribute attribute) throws AuthorizationException
	{
		String callerMethod = getCallerMethodName(2);
		LoginSession client = getVerifiedClient(AuthzCapability.attributeModify);
		long entityId = client.getEntityId();
		Set<AuthzRole> currentRoles = rolesResolver.establishRoles(entityId, new Group(attribute.getGroupPath()));
		Set<AuthzCapability> currentCapabilities = getRoleCapabilities(currentRoles, selfAccess);
		Set<AuthzRole> requestedRoles = rolesResolver.getRolesFromAttribute(attribute);
		Set<AuthzCapability> requestedCapabilities = getRoleCapabilities(requestedRoles, selfAccess);
		if (!currentCapabilities.containsAll(requestedCapabilities))
			throw new AuthorizationException("Access is denied. It is not allowed to set roles " + 
					"with higher privileges then those already possessed");
		if (!currentCapabilities.contains(AuthzCapability.attributeModify))
			throw new AuthorizationException("Access is denied. The operation " + 
					callerMethod + " requires '" + AuthzCapability.attributeModify + "' capability");
	}
	

	@Override
	@Transactional
	public Set<AuthzCapability> getCapabilities(boolean selfAccess, String group) throws AuthorizationException
	{
		LoginSession client = getVerifiedClient(new AuthzCapability[] {});
		return getCapabilities(getCallerMethodName(2), selfAccess, group, client);
	}
	
	private Set<AuthzCapability> getCapabilities(String callerMethod, boolean selfAccess, String groupPath,
			LoginSession client) throws AuthorizationException
	{
		Group group = groupPath == null ? new Group("/") : new Group(groupPath);
		Set<AuthzRole> roles = rolesResolver.establishRoles(client.getEntityId(), group);
		return getRoleCapabilities(roles, selfAccess);
	}
	
	private LoginSession getVerifiedClient(AuthzCapability... requiredCapabilities) throws AuthorizationException
	{
		LoginSession client = getCallerSession();
		
		//special case: if the credential is outdated, the only allowed operation is to update it
		//or read. Read is needed as to show credential update options it is needed to know the current state.
		if (client.isUsedOutdatedCredential())
		{
			if (requiredCapabilities.length > 1 || 
				(requiredCapabilities.length == 1 && 
						(requiredCapabilities[0] != AuthzCapability.credentialModify &&
						requiredCapabilities[0] != AuthzCapability.readInfo && 
						requiredCapabilities[0] != AuthzCapability.read)))
				throw new AuthorizationException("Access is denied. The client's credential " +
						"is outdated and the only allowed operation is the credential update");
		}
		return client;
	}
	
	private void checkAuthorizationInternal(String callerMethod, boolean selfAccess, String groupPath, 
			AuthzCapability... requiredCapabilities) throws AuthorizationException
	{
		LoginSession client = getVerifiedClient(requiredCapabilities);
		
		Set<AuthzCapability> capabilities = getCapabilities(callerMethod, selfAccess, groupPath, client);
		
		for (AuthzCapability requiredCapability: requiredCapabilities)
			if (!capabilities.contains(requiredCapability))
				throw new AuthorizationException("Access is denied. The operation " + 
						callerMethod + " requires '" + requiredCapability + "' capability");
	}
	
	@Override
	public boolean isSelf(long subject)
	{
		InvocationContext authnCtx = InvocationContext.getCurrent();
		return authnCtx.getLoginSession().getEntityId() == subject;
	}
	
	private Set<AuthzCapability> getRoleCapabilities(Set<AuthzRole> roles, boolean selfAccess)
	{
		Set<AuthzCapability> ret = new HashSet<AuthzCapability>();
		for (AuthzRole role: roles)
			Collections.addAll(ret, role.getCapabilities(selfAccess));
		return ret;
	}
	

	
	private String getCallerMethodName(int toSkipBackwards)
	{
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		int i = toSkipBackwards+1;
		while (i < stackTrace.length && 
				(stackTrace[i].getClassName().contains("Transaction") || 
				!stackTrace[i].getClassName().contains("pl.edu.icm.unity.")))
			i++;
		if (i >= stackTrace.length)
			return "UNKNOWN";
		return stackTrace[i].getMethodName();
	}

	@Override
	public void clearCache()
	{
		rolesResolver.clearCache();
	}

	@Override
	@Transactional
	public Set<AuthzRole> getRoles() throws AuthorizationException
	{
		LoginSession client = getCallerSession();
		return rolesResolver.establishRoles(client.getEntityId(), new Group("/"));
	}
	
	private LoginSession getCallerSession() throws AuthorizationException
	{
		InvocationContext authnCtx = InvocationContext.getCurrent();
		LoginSession client = authnCtx.getLoginSession();
		
		if (client == null)
			throw new AuthorizationException("Access is denied. The client is not authenticated.");
			
		return client;
	}
}
