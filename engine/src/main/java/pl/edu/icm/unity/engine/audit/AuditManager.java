/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.audit;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.audit.AuditEvent;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AuditEventManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManager;
import pl.edu.icm.unity.store.api.AuditEventDAO;
import pl.edu.icm.unity.base.tx.Transactional;

/**
 * Read access and management of audit log.
 *
 * @author R. Ledzinski
 */
@Component
@Transactional
public class AuditManager implements AuditEventManagement
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_AUDIT, AuditManager.class);

	private AuditEventDAO dao;
	private InternalAuthorizationManager authz;
	private AuditPublisher auditPublisher;
	private AuditEventListener auditEventListener;
	private volatile boolean publishingEnabled;

	@Autowired
	public AuditManager(AuditEventDAO dao, InternalAuthorizationManager authz,
						AuditPublisher auditPublisher, AuditEventListener auditEventListener, final UnityServerConfiguration mainConfig)
	{
		this.dao = dao;
		this.authz = authz;
		this.auditPublisher = auditPublisher;
		this.auditEventListener = auditEventListener;
		this.publishingEnabled = mainConfig.getBooleanValue(UnityServerConfiguration.AUDITEVENTLOGS_ENABLED);
		this.auditPublisher.enabled = this.publishingEnabled;
		this.auditEventListener.enabled = this.publishingEnabled;
		log.info("AuditEvents are {}", (this.publishingEnabled ? "enabled" : "disabled"));
	}

	@Override
	public List<AuditEvent> getAllEvents()
	{
		authz.checkAuthorizationRT("/", AuthzCapability.maintenance);
		return dao.getAll();
	}

	@Override
	public List<AuditEvent> getAuditEvents(final Date from, final Date until, final int limit, final String order, final int direction)
	{
		authz.checkAuthorizationRT("/", AuthzCapability.maintenance);
		return dao.getOrderedLogs(from, until, limit, order, direction);
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
		return publishingEnabled;
	}

	@Override
	public void enableAuditEvents()
	{
		this.publishingEnabled = true;
		auditPublisher.enabled = true;
		auditEventListener.enabled = true;
		auditEventListener.init();
		log.info("AuditEvents are enabled");
	}

	@Override
	public void disableAuditEvents()
	{
		this.publishingEnabled = false;
		auditPublisher.enabled = false;
		auditEventListener.enabled = false;
		log.info("AuditEvents are disabled");
	}
}
