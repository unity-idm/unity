/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.audit;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.CachingDAO;
import pl.edu.icm.unity.store.api.AuditEventDAO;
import pl.edu.icm.unity.store.rdbms.GenericRDBMSCRUD;
import pl.edu.icm.unity.types.basic.audit.AuditEvent;

import java.util.Set;

/**
 * RDBMS storage of {@link AuditEvent}
 * @author R. Ledzinski
 */
@Repository(AuditEventRDBMSStore.BEAN)
public class AuditEventRDBMSStore extends GenericRDBMSCRUD<AuditEvent, AuditEventBean>
					implements AuditEventDAO, CachingDAO
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, AuditEventRDBMSStore.class);
	public static final String BEAN = DAO_ID + "rdbms";

	@Autowired
	private AuditTagRDBMSStore tagDAO;

	@Autowired
	public AuditEventRDBMSStore(AuditEventJsonSerializer jsonSerializer)
	{
		super(AuditEventMapper.class, jsonSerializer, NAME);
	}

	@Override
	public long create(AuditEvent obj)
	{
		log.debug("New AuditEvent: {}", obj);
		long id = super.create(obj);
		if (obj.getTags() != null && obj.getTags().size() > 0) {
			tagDAO.insertAuditTags(id, obj.getTags());
		}

		return id;
	}

	@Override
	public void updateByKey(final long key, final AuditEvent obj) {
		super.updateByKey(key, obj);

		if (obj.getTags() != null && obj.getTags().size() > 0) {
			tagDAO.updateTags(key, obj.getTags());
		}
	}

	@Override
	public Set<String> getAllTags() {
		return tagDAO.getAllTags();
	}

	@Override
	public void invalidateCache() {
		tagDAO.invalidateCache();
	}
}
