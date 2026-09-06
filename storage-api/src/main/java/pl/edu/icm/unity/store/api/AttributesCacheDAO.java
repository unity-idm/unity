/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api;

import java.util.List;

import pl.edu.icm.unity.store.types.StoredAttribute;

/**
 * Stores materialized (effective) attribute values, i.e. a cache which shall mirror the results of the
 * effective attributes computation (direct + global + statement-produced attributes) for an entity in a group.
 */
public interface AttributesCacheDAO extends BasicCRUDDAO<StoredAttribute>
{
	String DAO_ID = "AttributesCacheDAO";
	String NAME = "attributes cache";

	/**
	 * Removes all cached attributes of the given entity in the given group.
	 */
	void deleteCacheInGroup(long entityId, String group);

	/**
	 * @return cached attributes of the given entity in the given group.
	 */
	List<StoredAttribute> getEntityAttributes(long entityId, String group);

	/**
	 * @return cached attributes of all entities in the given group.
	 */
	List<StoredAttribute> getGroupAttributes(String group);
}
