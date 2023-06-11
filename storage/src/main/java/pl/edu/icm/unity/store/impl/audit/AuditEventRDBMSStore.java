/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.audit;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import pl.edu.icm.unity.base.audit.AuditEvent;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.CachingDAO;
import pl.edu.icm.unity.store.api.AuditEventDAO;
import pl.edu.icm.unity.store.rdbms.GenericRDBMSCRUD;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionTL;

/**
 * RDBMS storage of {@link AuditEvent}
 * @author R. Ledzinski
 */
@Repository(AuditEventRDBMSStore.BEAN)
public class AuditEventRDBMSStore extends GenericRDBMSCRUD<AuditEvent, AuditEventBean>
					implements AuditEventDAO, CachingDAO
{
	private static final int LIMIT_MULTIPLIER = 2;
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, AuditEventRDBMSStore.class);
	public static final String BEAN = DAO_ID + "rdbms";

	private AuditTagRDBMSStore tagDAO;

	@Autowired
	public AuditEventRDBMSStore(final AuditEventJsonSerializer jsonSerializer, AuditTagRDBMSStore tagDAO)
	{
		super(AuditEventMapper.class, jsonSerializer, NAME);
		this.tagDAO = tagDAO;
	}

	@Override
	public List<AuditEvent> getOrderedLogs(final Date from, final Date until, int limit, final String sortOrder, final int direction)
	{
		log.trace("Getting logs from: {}, until: {}, limit: {}, Order: {}, dir: {}", from, until, limit, sortOrder, direction);

		AuditEventMapper mapper = SQLTransactionTL.getSql().getMapper(AuditEventMapper.class);
		// SQL returns duplicated AuditEvents, if there are multiple TAGS related to the event.
		// The assumption is that, there will be no more than 2 tags in average.
		List<AuditEvent> list = convertList(mapper.getOrderedLogs(from, until, limit * LIMIT_MULTIPLIER, AuditEventSortOrder.getByName(sortOrder).colName, direction < 0 ? "DESC" : "ASC"));
		if (list.size() > limit)
		{
			return list.subList(0, limit);
		} else
		{
			return list;
		}
	}

	@Override
	public long create(final AuditEvent obj)
	{
		log.debug("Inserting " + obj.toString());
		long id = super.create(obj);
		if (obj.getTags() != null && obj.getTags().size() > 0) 
			tagDAO.insertAuditTags(id, obj.getTags());

		return id;
	}

	@Override
	public void updateByKey(final long key, final AuditEvent obj) {
		throw new UnsupportedOperationException("Update operation is not supported for AuditEvents.");
	}

	@Override
	public Set<String> getAllTags() 
	{
		return tagDAO.getAllTags();
	}

	@Override
	public void invalidateCache() 
	{
		tagDAO.invalidateCache();
	}
}
