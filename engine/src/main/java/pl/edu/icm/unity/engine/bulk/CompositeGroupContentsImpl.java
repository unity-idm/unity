/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.bulk;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pl.edu.icm.unity.engine.api.bulk.CompositeGroupContents;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributesClass;
import pl.edu.icm.unity.types.basic.EntityInformation;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.Identity;

/**
 * Hidden implementation of the data backing bulk operations. Hidden as we do only some minimal checking
 * of that members are not modifiable. 
 * 
 * @author K. Benedyczak
 */
class CompositeGroupContentsImpl implements CompositeGroupContents
{
	private Map<String, AttributeType> attributeTypes;
	private Map<String, Group> groups;
	private Map<Long, EntityInformation> entityInfo;
	private Map<Long, List<Identity>> identities;
	private Map<Long, Map<String, Map<String, AttributeExt>>> directAttributes;
	private Map<String, AttributesClass> attributeClasses;
	private Map<Long, Set<String>> memberships;
	private Collection<CredentialDefinition> credentials;
	private Map<String, CredentialRequirements> credentialRequirements;
	
	private CompositeGroupContentsImpl() 
	{
	}
	
	public static Builder builder()
	{
		return new Builder();
	}
	
	public Map<String, AttributeType> getAttributeTypes()
	{
		return attributeTypes;
	}

	public Map<String, Group> getGroups()
	{
		return groups;
	}

	public Map<Long, EntityInformation> getEntityInfo()
	{
		return entityInfo;
	}

	public Map<Long, List<Identity>> getIdentities()
	{
		return identities;
	}

	public Map<Long, Map<String, Map<String, AttributeExt>>> getDirectAttributes()
	{
		return directAttributes;
	}

	public Map<String, AttributesClass> getAttributeClasses()
	{
		return attributeClasses;
	}

	public Map<Long, Set<String>> getMemberships()
	{
		return memberships;
	}

	public Map<String, CredentialRequirements> getCredentialRequirements()
	{
		return credentialRequirements;
	}

	public Collection<CredentialDefinition> getCredentials()
	{
		return credentials;
	}



	public static class Builder
	{
		CompositeGroupContentsImpl obj = new CompositeGroupContentsImpl();
		
		public Builder withAttributeTypes(Map<String, AttributeType> attributeTypes)
		{
			obj.attributeTypes = Collections.unmodifiableMap(attributeTypes);
			return this;
		}

		public Builder withGroups(Map<String, Group> groups)
		{
			obj.groups = Collections.unmodifiableMap(groups);
			return this;
		}
		
		public Builder withEntityInfo(Map<Long, EntityInformation> entityInfo)
		{
			obj.entityInfo = Collections.unmodifiableMap(entityInfo);
			return this;
		}
		
		public Builder withIdentities(Map<Long, List<Identity>> identities)
		{
			obj.identities = Collections.unmodifiableMap(identities);
			return this;
		}
		
		public Builder withDirectAttributes(Map<Long, Map<String, Map<String, AttributeExt>>> directAttributes)
		{
			obj.directAttributes = Collections.unmodifiableMap(directAttributes);
			return this;
		}
		
		public Builder withAttributeClasses(Map<String, AttributesClass> attributeClasses)
		{
			obj.attributeClasses = Collections.unmodifiableMap(attributeClasses);
			return this;
		}
		
		public Builder withMemberships(Map<Long, Set<String>> memberships)
		{
			obj.memberships = Collections.unmodifiableMap(memberships);
			return this;
		}
		
		public Builder withCredentialRequirements(Map<String, CredentialRequirements> credRequirements)
		{
			obj.credentialRequirements = Collections.unmodifiableMap(credRequirements);
			return this;
		}
		
		public Builder withCredentials(Collection<CredentialDefinition> credentials)
		{
			obj.credentials = Collections.unmodifiableCollection(credentials);
			return this;
		}
		
		public CompositeGroupContentsImpl build()
		{
			CompositeGroupContentsImpl ret = obj;
			obj = new CompositeGroupContentsImpl();
			return ret;
		}
	}
}
