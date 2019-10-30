/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.audit;

/**
 * Enum values for allowed AuditEvent sort order values.
 *
 * @author R. Ledzinski
 */
public enum AuditEventSortOrder {
	TIMESTAMP("EVENT.TIMESTAMP");

	public final String colName;

	AuditEventSortOrder(String colName) {
		this.colName = colName;
	}

	static AuditEventSortOrder getByName(String name) {
		for (AuditEventSortOrder sort : values())
			if (sort.name().equalsIgnoreCase(name))
				return sort;
		throw new IllegalArgumentException(String.join("", "Unsupported sort value '", "'"));
	}
};
