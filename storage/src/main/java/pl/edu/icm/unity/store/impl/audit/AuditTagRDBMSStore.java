/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.audit;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.HashSet;
import java.util.Set;

import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

import com.google.common.collect.Sets;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionTL;

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

	/**
	 * Impl note: tx savepoints are used to workaround postgres specific problem, which rollback complete transaction
	 * on error. As in the case of adding tag error is harmless we rollback only the failed insert.  
	 */
	private void insertTags(Set<String> tagList)
	{
		Set<String> missing = new HashSet<>(tagList);
		missing.removeAll(knownTags);
		if (missing.isEmpty())
			return;

		Connection connection = SQLTransactionTL.getSql().getConnection();
		AuditEventMapper mapper = SQLTransactionTL.getSql().getMapper(AuditEventMapper.class);
		try
		{
			for (String tag : missing)
			{
				Savepoint savepoint = connection.setSavepoint();
				try
				{
					mapper.createTag(tag);
					connection.releaseSavepoint(savepoint);
				} catch (PersistenceException e)
				{
					log.info("Can't add tag {}, it is already in db. Can happen but shouldn't happen often", tag);
					log.debug("Adding tag error details", e);
					connection.rollback(savepoint);
				}
			}
		} catch (SQLException e)
		{
			throw new PersistenceException(e);
		}
		knownTags.addAll(missing);
	}
}
