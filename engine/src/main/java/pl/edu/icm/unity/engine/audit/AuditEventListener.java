/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.audit;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.attributes.AttributeSupport;
import pl.edu.icm.unity.engine.api.event.EventListener;
import pl.edu.icm.unity.engine.notifications.email.EmailFacility;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.api.AuditEventDAO;
import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.store.types.StoredAttribute;
import pl.edu.icm.unity.types.AbstractEvent;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.EntityInformation;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.audit.AuditEntity;
import pl.edu.icm.unity.types.basic.audit.AuditEvent;

import java.util.List;

import static java.lang.String.join;

/**
 * Listens to AuditEvents and stores them in database.
 *
 * @author R. Ledzinski
 */
@Component
public class AuditEventListener implements EventListener
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, AuditEventListener.class);

	public static final String ID = AuditEventListener.class.getName();

	@Autowired
	private AuditManager auditManager;

	@Autowired
	private AuditEventDAO dao;

	@Autowired
	private TransactionalRunner tx;

	@Override
	public boolean isLightweight()
	{
		return true;
	}

	@Override
	public boolean isWanted(AbstractEvent event)
	{
		return (event instanceof AuditEventTrigger);
	}

	@Override
	public boolean isAsync(AbstractEvent event)
	{
		return true;
	}

	@Override
	public boolean handleEvent(AbstractEvent abstractEvent)
	{
		AuditEventTrigger event = (AuditEventTrigger) abstractEvent;
		log.debug("Got event: " + event);

        AuditEvent auditEvent = AuditEvent.builder()
                .type(event.getType())
				.action(event.getAction())
				.timestamp(event.getTimestamp())
                .name(event.getName())
                .subject(event.getSubjectEntity().orElseGet(()->auditManager.createAuditEntity(event.getSubjectEntityID())))
                .initiator(auditManager.createAuditEntity(event.getInitiatorEntityID()))
                .details(event.getDetails())
                .tags(event.getTags())
                .build();

        // Make sure only one event is stored in the same time - AuditEntities and Tags are shared resources
        synchronized (this)
		{
			tx.runInTransaction(() -> dao.create(auditEvent));
		}
		return true;
	}

	@Override
	public String getId()
	{
		return ID;
	}

	@Override
	public int getMaxFailures()
	{
		return 0;
	}
}
