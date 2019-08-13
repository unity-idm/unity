/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api;

import pl.edu.icm.unity.types.basic.audit.AuditEvent;

import java.util.Date;
import java.util.List;
import java.util.Set;

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
	 * @return AuditEvent list sorted by timestamp.
	 */
	List<AuditEvent> getLogs(final Date from, final Date until, final int limit);
}
