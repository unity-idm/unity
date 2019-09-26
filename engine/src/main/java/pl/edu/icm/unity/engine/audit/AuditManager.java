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
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManager;
import pl.edu.icm.unity.store.api.AuditEventDAO;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.basic.audit.AuditEvent;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Read access and management of audit log.
 *
 * @author R. Ledzinski
 */
@Component
@Transactional
public class AuditManager implements AuditEventManagement
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, AuditManager.class);
	private AuditEventDAO dao;
	private InternalAuthorizationManager authz;
	private AuditPublisher auditPublisher;


	@Autowired
	public AuditManager(AuditEventDAO dao, InternalAuthorizationManager authz,
						AuditPublisher auditPublisher)
	{
		this.dao = dao;
		this.authz = authz;
		this.auditPublisher = auditPublisher;
	}

	@Override
	public List<AuditEvent> getAllEvents()
	{
		authz.checkAuthorizationRT("/", AuthzCapability.maintenance);
		return dao.getAll();
	}

	@Override
	public List<AuditEvent> getAuditEvents(final Date from, final Date until, final int limit)
	{
		authz.checkAuthorizationRT("/", AuthzCapability.maintenance);
		return dao.getLogs(from, until, limit);
	}

	@Override
	public Set<String> getAllTags()
	{
		authz.checkAuthorizationRT("/", AuthzCapability.maintenance);
		return dao.getAllTags();
	}

	@Override
	public boolean isPublisherEnabled()
	{
		return auditPublisher.isEnabled();
	}
}
