/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.audit;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AuditEventManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.events.EventProcessor;
import pl.edu.icm.unity.store.api.AuditEventDAO;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.store.api.tx.TxManager;
import pl.edu.icm.unity.types.basic.audit.AuditEntity;
import pl.edu.icm.unity.types.basic.audit.AuditEvent;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Main interface to interact with {@link AuditEvent} storage layer.
 *
 * @author R. Ledzinski
 */
@Component
public class AuditManager implements AuditEventManagement
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, AuditManager.class);
	private static final AuditEntity SYSTEM_ENTITY = new AuditEntity(0L, "System", null);

	private EventProcessor eventProcessor;
	private AuditEventDAO dao;
	private TxManager txMan;
	private TransactionalRunner tx;

	@Autowired
	public AuditManager(final EventProcessor eventProcessor, final AuditEventDAO dao,
						final TxManager txMan, final TransactionalRunner tx)
	{
		this.eventProcessor = eventProcessor;
		this.dao = dao;
		this.txMan = txMan;
		this.tx = tx;
	}

	public void log(final AuditEventTrigger.AuditEventTriggerBuilder triggerBuilder) {
		if (InvocationContext.hasCurrent() &&
				InvocationContext.getCurrent().getLoginSession() != null)
		{
			triggerBuilder.initiator(InvocationContext.getCurrent().getLoginSession().getEntityId());
		} else
		{
			log.debug("Missing data in InvocationContext - using System initiator");
			triggerBuilder.initiator(SYSTEM_ENTITY);
		}
		txMan.addPostCommitAction(()->eventProcessor.fireEvent(triggerBuilder.build()));
	}

	@Override
	public List<AuditEvent> getAllEvents()
	{
		return tx.runInTransactionRet(() -> dao.getAll());
	}

	@Override
	public List<AuditEvent> getAuditEvents(final Date from, final Date until)
	{
		return tx.runInTransactionRet(() -> dao.getLogs(from, until));
	}

	@Override
	public Set<String> getAllTags()
	{
		return tx.runInTransactionRet(() -> dao.getAllTags());
	}
}
