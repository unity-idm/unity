/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.engine.api.bulk;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.Identity;

/**
 * Entity base membership information - contains entities groups, identities and attributes. Used in {@link BulkGroupQueryService}.
 * @author P.Piernik
 *
 */
public class GroupMembershipInfo
{
	public final long entityId;
	public final List<Identity> identities;
	public final Set<String> groups;
	public final Map<String, Map<String, AttributeExt>> attributes;
	
	public GroupMembershipInfo(long entityId, List<Identity> identities, Set<String> groups,
			 Map<String, Map<String, AttributeExt>> attributes)
	{
		this.entityId = entityId;
		this.identities = Collections.unmodifiableList(identities != null ? identities : Collections.emptyList());
		this.groups = Collections.unmodifiableSet(groups != null ? groups : Collections.emptySet());
		this.attributes = Collections.unmodifiableMap(attributes != null ? attributes : Collections.emptyMap());
	}
}
