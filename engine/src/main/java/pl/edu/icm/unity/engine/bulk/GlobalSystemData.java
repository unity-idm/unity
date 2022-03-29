/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.bulk;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import pl.edu.icm.unity.engine.api.mvel.CachingMVELGroupProvider;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributesClass;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.registration.EnquiryForm;

/**
 * Information about global state of the system, not group specific.
 */
class GlobalSystemData
{
	private Map<String, AttributeType> attributeTypes;
	private Map<String, Group> groups;
	private Map<String, AttributesClass> attributeClasses;
	private Collection<CredentialDefinition> credentials;
	private Map<String, CredentialRequirements> credentialRequirements;
	private Map<String, EnquiryForm> enquiryForms;
	private CachingMVELGroupProvider cachingMVELGroupProvider;
	
	private GlobalSystemData() 
	{
	}
	
	static Builder builder()
	{
		return new Builder();
	}
	
	Map<String, AttributeType> getAttributeTypes()
	{
		return attributeTypes;
	}

	Map<String, Group> getGroups()
	{
		return groups;
	}
	
	CachingMVELGroupProvider getCachingMVELGroupProvider()
	{
		return cachingMVELGroupProvider;
	}

	Map<String, AttributesClass> getAttributeClasses()
	{
		return attributeClasses;
	}

	Map<String, CredentialRequirements> getCredentialRequirements()
	{
		return credentialRequirements;
	}

	Collection<CredentialDefinition> getCredentials()
	{
		return credentials;
	}

	Map<String, EnquiryForm> getEnquiryForms()
	{
		return enquiryForms;
	}

	static class Builder
	{
		GlobalSystemData obj = new GlobalSystemData();
		
		Builder withAttributeTypes(Map<String, AttributeType> attributeTypes)
		{
			obj.attributeTypes = Collections.unmodifiableMap(attributeTypes);
			return this;
		}

		Builder withGroups(Map<String, Group> groups)
		{
			obj.groups = Collections.unmodifiableMap(groups);
			return this;
		}
		
		Builder withAttributeClasses(Map<String, AttributesClass> attributeClasses)
		{
			obj.attributeClasses = Collections.unmodifiableMap(attributeClasses);
			return this;
		}
		
		Builder withCredentialRequirements(Map<String, CredentialRequirements> credRequirements)
		{
			obj.credentialRequirements = Collections.unmodifiableMap(credRequirements);
			return this;
		}
		
		Builder withEnquiryForms(Map<String, EnquiryForm> enquiryForms)
		{
			obj.enquiryForms = Collections.unmodifiableMap(enquiryForms);
			return this;
		}
		
		Builder withCredentials(Collection<CredentialDefinition> credentials)
		{
			obj.credentials = Collections.unmodifiableCollection(credentials);
			return this;
		}
		
		GlobalSystemData build()
		{
			GlobalSystemData ret = obj;
			ret.cachingMVELGroupProvider = new CachingMVELGroupProvider(ret.groups);
			obj = new GlobalSystemData();
			return ret;
		}
	}
}
