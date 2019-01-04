/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.authz;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;

import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.Group;

/**
 * Returns roles for a user in a provided group. Results are cached.
 * 
 * @author K. Benedyczak
 */
class CachingRolesResolver
{
	private final Map<String, AuthzRole> roles; 
	private final AttributesHelper dbAttributes;
	private final Cache<CacheKey, Set<AuthzRole>> rolesCache;
	private final long cacheTTL;
	private final GroupDAO groupsDAO;
	
	CachingRolesResolver(Map<String, AuthzRole> roles, AttributesHelper dbAttributes, long cacheTTL, 
			GroupDAO groupsDAO)
	{
		this.roles = roles;
		this.dbAttributes = dbAttributes;
		this.cacheTTL = cacheTTL;
		this.groupsDAO = groupsDAO;
		this.rolesCache = CacheBuilder.newBuilder()
				.expireAfterWrite(cacheTTL, TimeUnit.MILLISECONDS)
				.build();
	}

	Set<AuthzRole> establishRoles(long entityId, Group group)
	{
		if (cacheTTL <= 0)
			return establishRolesNoCache(entityId, group);
		
		CacheKey cKey = new CacheKey(group, entityId);
		Set<AuthzRole> cached = rolesCache.getIfPresent(cKey);
		if (cached != null)
			return cached;
		Set<AuthzRole> actual = establishRolesNoCache(entityId, group);
		rolesCache.put(cKey, actual);
		return actual;
	}
	
	void clearCache()
	{
		rolesCache.invalidateAll();
	}
	
	private Set<AuthzRole> establishRolesNoCache(long entityId, Group group)
	{
		try
		{
			Group current = group;
			Set<AuthzRole> ret = new HashSet<>();
			do
			{
				Map<String, Map<String, AttributeExt>> allAttributes = getAuthzRoleAttributes(entityId, current);
				Map<String, AttributeExt> inCurrent = allAttributes.get(current.toString());
				if (inCurrent != null)
				{
					Attribute role = inCurrent.get(RoleAttributeTypeProvider.AUTHORIZATION_ROLE);
					ret.addAll(getRolesFromAttribute(role));
				}
				String parent = current.getParentPath();
				current = parent == null ? null : new Group(parent);
			} while (current != null);
			return ret;
		} catch (EngineException e)
		{
			throw new InternalException("Can't establish caller's roles", e);
		}
	}

	Set<AuthzRole> getRolesFromAttribute(Attribute role)
	{
		Set<AuthzRole> ret = new HashSet<>();
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
		return ret;
	}
	
	/**
	 * Retrieves the authz role for given entity in give group.
	 */
	private Map<String, Map<String, AttributeExt>> getAuthzRoleAttributes(long entityId, Group group) throws EngineException 
	{
		String groupPath = group.getName();
		
		if (!groupsDAO.exists(groupPath))
			return Maps.newHashMap();
		
		try
		{
			Map<String, Map<String, AttributeExt>> allAttributes = dbAttributes.getAllAttributesAsMap(
					entityId, groupPath, true, RoleAttributeTypeProvider.AUTHORIZATION_ROLE);
			return allAttributes;
		} catch (IllegalTypeException e)
		{
			throw new InternalException("Can't establish attributes for authorization pipeline", e);
		} catch (IllegalGroupValueException e)
		{
			throw new InternalException("Can't establish attributes for authorization pipeline - group problem", e);
		}
	}
	
	private static class CacheKey
	{
		private final Group group;
		private final long entityId;
		
		CacheKey(Group group, long entityId)
		{
			this.group = group;
			this.entityId = entityId;
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(entityId, group);
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CacheKey other = (CacheKey) obj;
			return entityId == other.entityId && Objects.equals(group, other.group);
		}
	}
}
