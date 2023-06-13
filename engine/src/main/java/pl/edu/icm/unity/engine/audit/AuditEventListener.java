/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.audit;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.audit.AuditEntity;
import pl.edu.icm.unity.base.audit.AuditEvent;
import pl.edu.icm.unity.base.event.Event;
import pl.edu.icm.unity.base.identity.EntityParam;
import pl.edu.icm.unity.base.identity.IllegalIdentityValueException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.attributes.AttributeSupport;
import pl.edu.icm.unity.engine.api.event.EventListener;
import pl.edu.icm.unity.engine.api.identity.UnknownIdentityException;
import pl.edu.icm.unity.engine.attribute.AttributeTypeChangedEvent;
import pl.edu.icm.unity.engine.notifications.email.EmailFacility;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.AuditEventDAO;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.store.types.StoredAttribute;

import java.util.List;

/**
 * Listens to AuditEvents and stores them in database.
 *
 * @author R. Ledzinski
 */
@Component
public class AuditEventListener implements EventListener
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_AUDIT, AuditEventListener.class);
	public static final String ID = AuditEventListener.class.getName();

	String entityNameAttribute;

	private AttributeDAO attributeDAO;
	private EmailFacility emailFacility;
	private AttributeSupport attributeSupport;
	private AuditEventDAO dao;
	private TransactionalRunner tx;
	volatile boolean enabled;

	@Autowired
	public AuditEventListener(final AttributeDAO attributeDAO, final EmailFacility emailFacility,
				final AttributeSupport attributeSupport, final AuditEventDAO dao,
				final TransactionalRunner tx)
	{
		this.attributeDAO = attributeDAO;
		this.emailFacility = emailFacility;
		this.dao = dao;
		this.tx = tx;
		this.attributeSupport = attributeSupport;
	}

	@Override
	public void init() 
	{
		if (!enabled) 
		{
			return;
		}
		initEntityNameAttributeFromDB();
	}

	private void initEntityNameAttributeFromDB()
	{
		AttributeType attr = null;
		try
		{
			attr = attributeSupport.getAttributeTypeWithSingeltonMetadata(
					EntityNameMetadataProvider.NAME);
		} catch (Exception e)
		{
			log.error("Failed to get attributeType", e);
		}
		entityNameAttribute = attr != null ? attr.getName() : null;
		log.debug("Entity name attribute set to: '" + entityNameAttribute + "'");
	}
	
	private void updateEntityNameAttributeIfNeeded(AttributeType attributeType)
	{
		if (attributeType.getMetadata().containsKey(EntityNameMetadataProvider.NAME))
		{
			entityNameAttribute = attributeType.getName();
		}
		
	}

	@Override
	public boolean isLightweight()
	{
		return true;
	}

	@Override
	public boolean isWanted(Event event)
	{
		return (event instanceof AuditEventTrigger) || (event instanceof AttributeTypeChangedEvent);
	}

	@Override
	public boolean isAsync(Event event)
	{
		// NOTE: Changing to synchronous processing requires changes in AuditManager.
		// It need to fire events directly instead of using tx.addPostCommitAction().
		return true;
	}

	@Override
	public boolean handleEvent(final Event abstractEvent)
	{
		if (!enabled)
			return true;
		if (abstractEvent instanceof AttributeTypeChangedEvent)
			return handleAttributeTypeChangeEvent((AttributeTypeChangedEvent)abstractEvent);
		if (abstractEvent instanceof AuditEventTrigger)
			return handleAuditEventTrigger((AuditEventTrigger)abstractEvent);

		log.error("Unexpected event type, verify isWanted() method implementation");
		return false;
	}

	private boolean handleAuditEventTrigger(AuditEventTrigger event) 
	{
		AuditEvent auditEvent = AuditEvent.builder()
				.type(event.getType())
				.action(event.getAction())
				.timestamp(event.getTimestamp())
				.name(event.getName())
				.subject(event.getSubjectEntity().orElseGet(() -> createAuditEntity(event.getSubjectEntityID())))
				.initiator(event.getInitiatorEntity().orElseGet(() -> createAuditEntity(event.getInitiatorEntityID())))
				.details(event.getDetails())
				.tags(event.getTags())
				.build();

		// Make sure only one event is stored in the same time - AuditEntities and Tags are shared resources.
		// Transaction retry is also addressing this problem - synchronized block is for code optimization.
		synchronized (this)
		{
			tx.runInTransaction(() -> dao.create(auditEvent));
		}
		return true;
	}

	private boolean handleAttributeTypeChangeEvent(AttributeTypeChangedEvent event) 
	{
		if (event.oldAT == null) 
		{
			// New attribute created
			updateEntityNameAttributeIfNeeded(event.newAT);
				
		} else if (event.newAT == null) 
		{
			// Attribute was removed
			if (event.oldAT.getMetadata().containsKey(EntityNameMetadataProvider.NAME))
				initEntityNameAttributeFromDB();
		} else if (!event.oldAT.getMetadata().containsKey(EntityNameMetadataProvider.NAME) && event.newAT.getMetadata().containsKey(EntityNameMetadataProvider.NAME)) 
		{
			//EntityNameMetadataProvider.NAME was added to attribute
			updateEntityNameAttributeIfNeeded(event.newAT);
		} else if (event.oldAT.getMetadata().containsKey(EntityNameMetadataProvider.NAME) && !event.newAT.getMetadata().containsKey(EntityNameMetadataProvider.NAME))
		{
			//EntityNameMetadataProvider.NAME was removed from attribute
			initEntityNameAttributeFromDB();
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

	public AuditEntity createAuditEntity(final Long entityId)
	{
		if (entityId == null)
			return null;
		return tx.runInTransactionRet(() -> 
		{
			String email = null;
			try
			{
				email = emailFacility.getAddressForEntity(new EntityParam(entityId), null, false);
			} catch (IllegalIdentityValueException e)
			{
				log.debug("No email address for entityId={}", entityId);
			} catch (UnknownIdentityException e)
			{
				log.debug("entityId={} was already removed from system", entityId);
			} catch (Exception e)
			{
				log.error("Error getting email for entityId=" + entityId + ", exception:", e);
			}

			String name = null;
			if (entityNameAttribute != null) 
			{
				List<StoredAttribute> attrs = attributeDAO.getAttributes(entityNameAttribute, entityId, null);
				name = attrs.size() > 0 ? attrs.get(0).getAttribute().getValues().get(0) : null;
			}

			return new AuditEntity(entityId, name, email);
		});
	}
}
