/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.audit;

import org.apache.ibatis.annotations.Param;
import pl.edu.icm.unity.store.rdbms.BasicCRUDMapper;

import java.util.Set;


/**
 * Access to the AuditEvent.xml operations.
 * @author R.Ledzinski
 */
public interface AuditEventMapper extends BasicCRUDMapper<AuditEventBean>
{
	Long getAuditEntityId(AuditEntityBean bean);
	long createAuditEntity(AuditEntityBean bean);

	Set<String> getAllTags();
	void createTags(@Param("tagList") Set<String> tagList);

	void insertAuditTags(@Param("eventId") long eventId, @Param("tagList") Set<String> tags);
	void deleteTags(long eventId);
}
