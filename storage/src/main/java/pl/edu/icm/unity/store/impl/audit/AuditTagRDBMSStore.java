/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.audit;

import com.google.common.collect.Sets;
import org.springframework.stereotype.Repository;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionTL;

import java.util.HashSet;
import java.util.Set;

/**
 * RDBMS storage of AuditEvent Tags. Helper repository to handle actions on Tag related tables entries.
 * <p>
 * Package private access - public methods are exposed via AuditEventDAO.
 *
 * @author R. Ledzinski
 */
@Repository
class AuditTagRDBMSStore
{
	private Set<String> knownTags = Sets.newConcurrentHashSet();

	void invalidateCache()
	{
		knownTags.clear();
	}

	Set<String> getAllTags()
	{

		AuditEventMapper mapper = SQLTransactionTL.getSql().getMapper(AuditEventMapper.class);
		Set<String> allTags = mapper.getAllTags();
		knownTags.addAll(allTags);
		return allTags;
	}

	void insertAuditTags(long eventId, Set<String> tagList)
	{
		// Make sure all tags are in DB
		insertTags(tagList);
		// Add tags for given event
		AuditEventMapper mapper = SQLTransactionTL.getSql().getMapper(AuditEventMapper.class);
		mapper.insertAuditTags(eventId, tagList);
	}

	private void insertTags(Set<String> tagList)
	{
		Set<String> missing = new HashSet<>(tagList);
		missing.removeAll(knownTags);
		if (missing.isEmpty()) {
			return;
		}
		AuditEventMapper mapper = SQLTransactionTL.getSql().getMapper(AuditEventMapper.class);
		mapper.createTags(missing);
		knownTags.addAll(missing);
	}
}
