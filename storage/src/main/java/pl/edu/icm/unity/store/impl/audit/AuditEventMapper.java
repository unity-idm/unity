/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.audit;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.ibatis.annotations.Param;

import pl.edu.icm.unity.store.rdbms.BasicCRUDMapper;


/**
 * Access to the AuditEvent.xml operations.
 * @author R.Ledzinski
 */
public interface AuditEventMapper extends BasicCRUDMapper<AuditEventBean>
{
	Long getAuditEntityId(AuditEntityBean bean);
	long createAuditEntity(AuditEntityBean bean);

	Set<String> getAllTags();
	void createTag(@Param("tag") String tag);

	void insertAuditTags(@Param("eventId") long eventId, @Param("tagList") Set<String> tags);

	List<AuditEventBean> getOrderedLogs(@Param("from") Date from, @Param("until") Date until, @Param("limit") int limit,
										@Param("order") String order, @Param("direction") String direction);
}
