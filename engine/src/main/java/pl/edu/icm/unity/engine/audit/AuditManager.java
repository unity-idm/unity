/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.audit;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.attributes.AttributeSupport;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.events.EventProcessor;
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
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.EntityInformation;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.audit.AuditEntity;
import pl.edu.icm.unity.types.basic.audit.AuditEvent;
import pl.edu.icm.unity.types.basic.audit.EventAction;
import pl.edu.icm.unity.types.basic.audit.EventType;

import java.util.List;
import java.util.Map;

import static java.lang.String.join;

/**
 * Main interface to interact with {@link AuditEvent} storage layer.
 *
 * @author R. Ledzinski
 */
@Component
public class AuditManager implements InitializingBean
{
    private static final Logger log = Log.getLogger(Log.U_SERVER, AuditManager.class);
    private static final String NULL_STR = "<null>";

    private String entityNameAttribute;

    @Autowired
    EventProcessor eventProcessor;

    @Autowired
    AuditEventDAO dao;

    @Autowired
    private AttributeTypeDAO attributeTypeDAO;

    @Autowired
    private AttributeDAO attributeDAO;

    @Autowired
    private EmailFacility emailFacility;

    @Autowired
    private EntityDAO entityDAO;

    @Autowired
    private AttributeSupport attributeSupport;

    @Autowired
    private TransactionalRunner tx;

    @Override
    public void afterPropertiesSet()
    {
        AttributeType attr = null;
        try
        {
            attr = attributeSupport.getAttributeTypeWithSingeltonMetadata(
                    EntityNameMetadataProvider.NAME);
        } catch (Exception e)
        {
            log.error("Cannot retrieve attributeType", e);
        }
        if (attr == null)
        {
            entityNameAttribute = "name";
        } else
        {
            entityNameAttribute = attr.getName();
        }
    }

    public void fireEvent(final EventType type, final EventAction action,
                          final String subjectName, final long subjectEntityId,
                          final Map<String, String> details, final String ... tags)
    {
        if (InvocationContext.hasCurrent() &&
                InvocationContext.getCurrent().getLoginSession() != null)
        {
            eventProcessor.fireEvent(new AuditEventTrigger(
                    type,
                    action,
                    subjectName,
                    subjectEntityId,
                    InvocationContext.getCurrent().getLoginSession().getEntityId(),
                    details,
                    tags
            ));
        } else
        {
            log.error("Cannot generate AuditEvent - missing data in InvocationContext");
        }
    }

    public void fireEvent(final EventType type, final EventAction action,
                          final String subjectName, final AuditEntity subjectEntity,
                          final Map<String, String> details, final String ... tags)
    {
        if (InvocationContext.hasCurrent() &&
                InvocationContext.getCurrent().getLoginSession() != null)
        {
            eventProcessor.fireEvent(new AuditEventTrigger(
                    type,
                    action,
                    subjectName,
                    subjectEntity,
                    InvocationContext.getCurrent().getLoginSession().getEntityId(),
                    details,
                    tags
            ));
        } else
        {
            log.error("Cannot generate AuditEvent - missing data in InvocationContext");
        }
    }

    public AuditEntity createAuditEntity(Long entityId)
    {
        if (entityId == null)
        {
            return null;
        }
        return tx.runInTransactionRet(() -> {
            String email = NULL_STR;
            try
            {
                email = emailFacility.getAddressForEntity(new EntityParam(entityId), null, false);
            } catch (IllegalIdentityValueException e)
            {
                log.debug("No email address for entityId={}", entityId);
            } catch (EngineException e)
            {
                log.error("Error getting email for entityId={}", entityId);
            }

            String name;
            List<StoredAttribute> attrs = attributeDAO.getAttributes(entityNameAttribute, entityId, null);
            if (attrs.size() > 0)
            {
                name = join("",attrs.get(0).getAttribute().getValues().get(0), " ", "[", Long.toString(entityId), "]");
            }
            else
            {
                name = join("","[", Long.toString(entityId), "]");
            }

            return new AuditEntity(entityId, name, email);
        });
    }

    public List<AuditEvent> getAllEvents()
    {
        return tx.runInTransactionRet(() -> dao.getAll());
    }
}
