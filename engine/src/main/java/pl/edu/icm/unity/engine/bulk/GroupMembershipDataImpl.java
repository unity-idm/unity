/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.bulk;

import pl.edu.icm.unity.engine.api.bulk.GroupMembershipData;

/**
 * Hidden implementation of the data backing bulk operations. Hidden as we do only some minimal checking
 * of that members are not modifiable. 
 */
class GroupMembershipDataImpl implements GroupMembershipData
{
	final String group;
	final GlobalSystemData globalSystemData;
	final EntitiesData entitiesData;
	
	GroupMembershipDataImpl(String group, GlobalSystemData globalSystemData, EntitiesData entitiesData)
	{
		this.group = group;
		this.globalSystemData = globalSystemData;
		this.entitiesData = entitiesData;
	}
}
