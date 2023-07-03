/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.bulk;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.entity.EntityInformation;
import pl.edu.icm.unity.base.identity.Identity;

/**
 * Information about many entities. May include all system entities, or only a subset, depending on usage context.
 */
class EntitiesData
{
	private Map<Long, EntityInformation> entityInfo;
	private Map<Long, List<Identity>> identities;
	private Map<Long, Map<String, Map<String, AttributeExt>>> directAttributes;
	private Map<Long, Set<String>> memberships;
	
	private EntitiesData() 
	{
	}
	
	static Builder builder()
	{
		return new Builder();
	}
	
	Map<Long, EntityInformation> getEntityInfo()
	{
		return entityInfo;
	}

	Map<Long, List<Identity>> getIdentities()
	{
		return identities;
	}

	Map<Long, Map<String, Map<String, AttributeExt>>> getDirectAttributes()
	{
		return directAttributes;
	}

	Map<Long, Set<String>> getMemberships()
	{
		return memberships;
	}

	static class Builder
	{
		EntitiesData obj = new EntitiesData();
		
		private Builder()
		{
		}
		
		Builder withEntityInfo(Map<Long, EntityInformation> entityInfo)
		{
			obj.entityInfo = Collections.unmodifiableMap(entityInfo);
			return this;
		}
		
		Builder withIdentities(Map<Long, List<Identity>> identities)
		{
			obj.identities = Collections.unmodifiableMap(identities);
			return this;
		}
		
		Builder withDirectAttributes(Map<Long, Map<String, Map<String, AttributeExt>>> directAttributes)
		{
			obj.directAttributes = Collections.unmodifiableMap(directAttributes);
			return this;
		}
		
		Builder withMemberships(Map<Long, Set<String>> memberships)
		{
			obj.memberships = Collections.unmodifiableMap(memberships);
			return this;
		}
		
		EntitiesData build()
		{
			EntitiesData ret = obj;
			obj = new EntitiesData();
			return ret;
		}
	}
}
