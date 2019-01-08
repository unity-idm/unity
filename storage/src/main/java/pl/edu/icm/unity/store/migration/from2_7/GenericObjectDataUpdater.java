/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.migration.from2_7;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Used to pass through the implementation of a migration code for a single
 * generic object.
 */
@FunctionalInterface
interface GenericObjectDataUpdater
{
	/**
	 * Updates the generic object in json form, which is provided by migration
	 * infrastructure.
	 * 
	 * @param content
	 *            object node from previous release
	 * @return migrated object
	 */
	ObjectNode update(ObjectNode content);
}
