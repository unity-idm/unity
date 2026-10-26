/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attribute;

import java.util.List;

/**
 * Access to the AttributesCachePending.xml operations.
 */
public interface AttributesCachePendingMapper
{
	void create(AttributesCachePendingBean obj);

	int incrementVersion(AttributesCachePendingBean param);

	int incrementVersionForGroupMembers(long groupId);

	int createMissingForGroupMembers(AttributesCachePendingBean param);

	int incrementVersionOfAll();

	int createMissingForAllMemberships(AttributesCachePendingBean param);

	int deleteByKeyAndVersion(AttributesCachePendingBean param);

	void deleteByGroup(long groupId);

	AttributesCachePendingBean getByKey(AttributesCachePendingBean param);

	List<AttributesCachePendingBean> getAll();

	List<AttributesCachePendingBean> getByGroup(long groupId);
}
