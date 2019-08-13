/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api;

import pl.edu.icm.unity.types.basic.audit.AuditEvent;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * AuditEvent management API.
 *
 * @author R. Ledzinski
 */
public interface AuditEventManagement
{
	/**
	 * List of AuditEvent objects.
	 * @return all AuditEvent sorted by timestamp .
	 */
	List<AuditEvent> getAllEvents();

	/**
	 * Retrieve list of AuditEvents sorted by timestamp for given time period.
	 * @param from
	 * 		From date or from the earliest timestamp (if null)
	 * @param until
	 * 		Until date or till the latest timestamp (if null)
	 * @param limit
	 * 		Maximum number of returned records
	 * @return AuditEvent list sorted by timestamp.
	 */
	List<AuditEvent> getAuditEvents(final Date from, final Date until, final int limit);

	/**
	 * List of tags.
	 * @return all Tags sorted by name.
	 */
	Set<String> getAllTags();
}
