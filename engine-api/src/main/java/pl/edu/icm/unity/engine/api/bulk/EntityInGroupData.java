/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.engine.api.bulk;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;

import java.util.Map;
import java.util.Set;

import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.entity.Entity;

/**
 * Almost all information about a single entity in general plus attributes in a particular group and in root group
 */
public class EntityInGroupData
{
	public final Entity entity;
	public final Set<String> groups;
	public final Set<String> relevantEnquiryForms;
	public final Map<String, AttributeExt> groupAttributesByName;
	public final Map<String, AttributeExt> rootAttributesByName;
	public final String attributesGroup;
	
	public EntityInGroupData(Entity entity, String group, Set<String> groups,
			 Map<String, AttributeExt> groupAttributes, 
			 Map<String, AttributeExt> rootAttributes, Set<String> relevantEnquiryForm)
	{
		this.entity = entity;
		this.attributesGroup = group;
		this.groups = groups != null ? unmodifiableSet(groups) : emptySet();
		this.groupAttributesByName = groupAttributes != null ? unmodifiableMap(groupAttributes) : emptyMap();
		this.rootAttributesByName = rootAttributes != null ? unmodifiableMap(rootAttributes) : emptyMap();
		this.relevantEnquiryForms = relevantEnquiryForm != null ? unmodifiableSet(relevantEnquiryForm) : emptySet();
	}
}
