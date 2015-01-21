/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.authz;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBAttributes;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.sysattrs.SystemAttributeTypes;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.Group;


/**
 * Default implementation of the {@link AuthorizationManager}
 * @author K. Benedyczak
 */
@Component
@Primary
public class AuthorizationManagerImpl implements AuthorizationManager
{
	private Map<String, AuthzRole> roles = new LinkedHashMap<String, AuthzRole>(); 

	private DBSessionManager db;
	private DBAttributes dbAttributes;
	/**
	 * System manager role with all privileges. Must not be removed or modified.
	 */
	public static final String SYSTEM_MANAGER_ROLE = "System Manager";

	public static final String CONTENTS_MANAGER_ROLE = "Contents Manager";  
	public static final String PRIVILEGED_INSPECTOR_ROLE = "Privileged Inspector";
	public static final String INSPECTOR_ROLE = "Inspector";
	public static final String USER_ROLE = "Regular User";
	public static final String ANONYMOUS_ROLE = "Anonymous User";
			
	@Autowired
	public AuthorizationManagerImpl(DBSessionManager db, DBAttributes dbAttributes)
	{
		this.db = db;
		this.dbAttributes = dbAttributes;
		setupRoleCapabilities();
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
					AuthzCapability.read
				}));
		
		setupRole(new RoleImpl(USER_ROLE, "Allows owners for reading of the basic system information," +
				" retrieval of information about themselves and also for changing passwords " +
				"and self managed attributes", 
				new AuthzCapability[] {
					AuthzCapability.readInfo
				},
				new AuthzCapability[] {
					AuthzCapability.credentialModify,
					AuthzCapability.attributeModify,
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
	public void checkAuthorization(AuthzCapability... requiredCapabilities) throws AuthorizationException
	{
		checkAuthorizationInternal(getCallerMethodName(2), false, null, requiredCapabilities);
	}

	@Override
	public void checkAuthorization(boolean selfAccess, AuthzCapability... requiredCapabilities) throws AuthorizationException
	{
		checkAuthorizationInternal(getCallerMethodName(2), selfAccess, null, requiredCapabilities);
	}
	
	@Override
	public void checkAuthorization(String group, AuthzCapability... requiredCapabilities) throws AuthorizationException
	{
		checkAuthorizationInternal(getCallerMethodName(2), false, group, requiredCapabilities);
	}

	@Override
	public void checkAuthorization(boolean selfAccess, String groupPath, AuthzCapability... requiredCapabilities) throws AuthorizationException
	{
		checkAuthorizationInternal(getCallerMethodName(2), selfAccess, groupPath, requiredCapabilities);
	}
	
	private void checkAuthorizationInternal(String callerMethod, boolean selfAccess, String groupPath, 
			AuthzCapability... requiredCapabilities) throws AuthorizationException
	{
		Group group = groupPath == null ? new Group("/") : new Group(groupPath);
		InvocationContext authnCtx = InvocationContext.getCurrent();
		LoginSession client = authnCtx.getLoginSession();

		if (client == null)
			throw new AuthorizationException("Access is denied. The client is not authenticated.");
		
		//special case: if the credential is outdated, the only allowed operation is to update it
		//or read. Read is needed as to show credential update options it is needed to know the current state.
		if (client.isUsedOutdatedCredential())
		{
			if (requiredCapabilities.length > 1 || 
					(requiredCapabilities[0] != AuthzCapability.credentialModify &&
					requiredCapabilities[0] != AuthzCapability.readInfo && 
					requiredCapabilities[0] != AuthzCapability.read))
				throw new AuthorizationException("Access is denied. The client's credential " +
						"is outdated and the only allowed operation is the credential update");
		}
		
		Set<AuthzRole> roles;
		try
		{
			roles = establishRoles(client.getEntityId(), group);
		} catch (EngineException e)
		{
			throw new InternalException("Can't establish caller's roles", e);
		}
		Set<AuthzCapability> capabilities = getRoleCapabilities(roles, selfAccess);
		
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
	
	private Set<AuthzRole> establishRoles(long entityId, Group group) throws EngineException
	{
		Map<String, Map<String, AttributeExt<?>>> allAttributes = getAllAttributes(entityId);
		Group current = group;
		Set<AuthzRole> ret = new HashSet<AuthzRole>();
		do
		{
			Map<String, AttributeExt<?>> inCurrent = allAttributes.get(current.toString());
			if (inCurrent != null)
				addRolesFromAttribute(inCurrent, ret);
			String parent = current.getParentPath();
			current = parent == null ? null : new Group(parent);
		} while (current != null);
		return ret;
	}

	private void addRolesFromAttribute(Map<String, AttributeExt<?>> inCurrent, Set<AuthzRole> ret)
	{
		Attribute<?> role = inCurrent.get(SystemAttributeTypes.AUTHORIZATION_ROLE);
		if (role != null)
		{
			List<?> roles = role.getValues();
			for (Object r: roles)
			{
				AuthzRole rr = this.roles.get(r.toString());
				if (rr == null)
					throw new InternalException("Authorization attribute has " +
							"unsupported role value: " + r);
				ret.add(rr);
			}
		}
	}
	
	private Map<String, Map<String, AttributeExt<?>>> getAllAttributes(long entityId) throws EngineException 
	{
		SqlSession sql = db.getSqlSession(true);
		try
		{
			Map<String, Map<String, AttributeExt<?>>> allAttributes = 
					dbAttributes.getAllAttributesAsMap(entityId, null, true, null, sql);
			sql.commit();
			return allAttributes;
		} catch (IllegalTypeException e)
		{
			throw new InternalException("Can't establish attributes for authorization pipeline", e);
		} catch (IllegalGroupValueException e)
		{
			throw new InternalException("Can't establish attributes for authorization pipeline - group problem", e);
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}
	
	private String getCallerMethodName(int backwards)
	{
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		if (stackTrace.length < backwards+2)
			return "UNKNOWN";
		return stackTrace[backwards+1].getMethodName();
	}
}
