/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api;

import java.util.Date;
import java.util.List;
import java.util.Set;

import pl.edu.icm.unity.base.audit.AuditEvent;

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
	 * @param order
	 * 		AuditEvent field for sorting purposes - at this moment only "timestamp" value is supported
	 * @param direction
	 * 		Descending in case of negative value, ascending order in other cases.
	 * @return AuditEvent list sorted by timestamp.
	 */
	List<AuditEvent> getAuditEvents(final Date from, final Date until, final int limit, final String order, final int direction);

	/**
	 * List of tags.
	 * @return all Tags sorted by name.
	 */
	Set<String> getAllTags();

	/**
	 * Checks, if Audit Logs feature (Audit Logs gathering) in enabled in the system.
	 * @return Audit Logs feature status
	 */
	boolean isPublisherEnabled();

	/**
	 * Enable Audit Logs feature (Audit Logs gathering) in the system.
	 */
	void enableAuditEvents();

	/**
	 * Disable Audit Logs feature (Audit Logs gathering) n the system.
	 */
	void disableAuditEvents();
}
