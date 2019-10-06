/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.audit;

import com.google.common.collect.Sets;

import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionTL;

import java.sql.SQLIntegrityConstraintViolationException;
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
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, AuditTagRDBMSStore.class);
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
		if (missing.isEmpty())
			return;

		AuditEventMapper mapper = SQLTransactionTL.getSql().getMapper(AuditEventMapper.class);
		for (String tag : missing)
		{
			try
			{
				mapper.createTag(tag);
			} catch (PersistenceException e)
			{
				if (e.getCause() instanceof SQLIntegrityConstraintViolationException)
				{
					log.info("Can't add tag {}, it is already in db. Can happen but shouldn't happen often");
					log.debug("Adding tag error details", e);
				} else
				{
					throw e;
				}
			}
		}
		knownTags.addAll(missing);
	}
}
