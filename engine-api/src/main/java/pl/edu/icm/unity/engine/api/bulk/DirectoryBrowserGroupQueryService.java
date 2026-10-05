/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.bulk;

import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.exceptions.EngineException;

import java.util.Map;

/**
 * Lean counterpart of {@link BulkGroupQueryService}, dedicated to listing entities of a group (e.g. in the
 * directory browser) without resolving their effective/dynamic attributes. Loads only what is needed to
 * assemble {@link Entity} objects (identities, entity state, credential status) - not custom attribute
 * values, attribute types/classes, enquiry forms or cross-group memberships.
 */
public interface DirectoryBrowserGroupQueryService
{
	GroupMembershipData getGroupListingData(String group) throws EngineException;

	Map<Long, Entity> getGroupEntitiesNoContextWithTargeted(GroupMembershipData data);

	Map<Long, Entity> getGroupEntitiesNoContextWithoutTargeted(GroupMembershipData data);
}
