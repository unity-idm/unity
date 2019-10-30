/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.bulk;

import java.util.Collections;
import java.util.Set;

class MultiGroupMembershipData
{
	final Set<String> groups;
	final GlobalSystemData globalSystemData;
	final EntitiesData entitiesData;

	MultiGroupMembershipData(Set<String> groups, GlobalSystemData globalSystemData, EntitiesData entitiesData)
	{
		this.globalSystemData = globalSystemData;
		this.entitiesData = entitiesData;
		this.groups = Collections.unmodifiableSet(groups);
	}
}
