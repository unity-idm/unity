/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api;

import java.util.Date;
import java.util.List;
import java.util.Set;

import pl.edu.icm.unity.base.audit.AuditEvent;

/**
 * AuditEvent DAO
 * @author R. Ledzinski
 */
public interface AuditEventDAO extends BasicCRUDDAO<AuditEvent>
{
	String DAO_ID = "AuditEventDAO";
	String NAME = "Audit event";

	/**
	 * List of available Tags.
	 * @return all {@link AuditEvent} objects sorted by timestamp.
	 */
	Set<String> getAllTags();

	/**
	 * Retrieve list of AuditEvents sorted by timestamp for given time period.
	 * @param from
	 * 		From date or from the earliest timestamp (if null)
	 * @param until
	 * 		Until date or till the latest timestamp (if null)
	 * @param limit
	 * 		Maximum number of returned records
	 * @return AuditEvent list sorted by timestamp descending.
	 */
	default List<AuditEvent> getLogs(final Date from, final Date until, final int limit) {
		return getOrderedLogs(from, until, limit, "timestamp", -1);
	}

	/**
	 * Retrieve list of AuditEvents for given time period, sorted by order param (and secondly by timestamp descending) with given direction.
	 * @param from
	 * 		From date or from the earliest timestamp (if null)
	 * @param until
	 * 		Until date or till the latest timestamp (if null)
	 * @param limit
	 * 		Maximum number of returned records
	 * @param order
	 * 		AuditEvent field for sorting purposes - at this moment only "timestamp" value is supported
	 * @param direction
	 * 		Descending in case of negative value, ascending order in other cases.
	 * @return AuditEvent list
	 */
	List<AuditEvent> getOrderedLogs(final Date from, final Date until, final int limit, final String order, final int direction);
}
