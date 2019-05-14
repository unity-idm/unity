/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.audit;

import com.google.common.collect.Sets;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.store.api.AuditEventDAO;
import pl.edu.icm.unity.store.impl.AbstractBasicDAOTest;
import pl.edu.icm.unity.store.tx.TransactionTL;
import pl.edu.icm.unity.types.basic.audit.AuditEntity;
import pl.edu.icm.unity.types.basic.audit.AuditEvent;
import pl.edu.icm.unity.types.basic.audit.EventAction;
import pl.edu.icm.unity.types.basic.audit.EventType;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AuditEventTest extends AbstractBasicDAOTest<AuditEvent>
{
    @Autowired
    private AuditEventDAO dao;

    @Autowired
    AuditTagRDBMSStore tagDAO;

    @Override
    protected AuditEventDAO getDAO()
    {
        return dao;
    }

    @Override
    protected AuditEvent getObject(String id)
    {
        return AuditEvent.builder()
                .name("name")
                .type(EventType.IDENTITY)
                .timestamp(new Date())
                .action(EventAction.ADD)
                .details(JsonUtil.parse("{\"comment\" : \"No comment\"}"))
                .subject(new AuditEntity(101l, "Subject", "subject@example.com"))
                .initiator(new AuditEntity(100l, "Initiator", "initiator@example.com"))
                .tags("TAG1", "TAG2")
                .build();
    }

    @Override
    protected AuditEvent mutateObject(AuditEvent src)
    {
        return AuditEvent.builder()
                .name("name2")
                .type(EventType.IDENTITY)
                .timestamp(new Date())
                .action(EventAction.UPDATE)
                .details(JsonUtil.parse("{\"comment\" : \"No new comment\"}"))
                .subject(new AuditEntity(102l, "Subject2", "subject2@example.com"))
                .initiator(src.getInitiator())
                .tags("TAG2", "TAG3")
                .build();
    }

    @Test
    public void shouldStoreEventWithNulls()
    {
        AuditEvent event = AuditEvent.builder()
                .name("name2")
                .type(EventType.IDENTITY)
                .timestamp(new Date())
                .action(EventAction.UPDATE)
                .initiator(new AuditEntity(100l, "Initiator", "initiator@example.com"))
                .build();
        tx.runInTransaction(() -> {
            long id = dao.create(event);
            TransactionTL.manualCommit();
            AuditEvent eventFromDB = dao.getByKey(id);
            assertEquals(event, eventFromDB);
            assertTrue(eventFromDB.getDetails() == null);
            assertTrue(eventFromDB.getSubject() == null);
            assertTrue(eventFromDB.getTags().isEmpty());
        });
    }

    @Test
    public void shouldReturnAllEventsAndTags()
    {
        AuditEvent event1 = AuditEvent.builder()
                .name("Identity name")
                .type(EventType.IDENTITY)
                .timestamp(new Date())
                .action(EventAction.ADD)
                .initiator(new AuditEntity(100l, "Initiator", "initiator@example.com"))
                .details(JsonUtil.parse("{\"comment\" : \"No comment\"}"))
                .subject(new AuditEntity(101l, "Subject", "subject@example.com"))
                .tags("TAG1", "TAG2")
                .build();
        AuditEvent event2 = AuditEvent.builder()
                .name("Identity name")
                .type(EventType.IDENTITY)
                .timestamp(new Date())
                .action(EventAction.UPDATE)
                .initiator(new AuditEntity(100l, "Initiator", "initiator@example.com"))
                .details(JsonUtil.parse("{\"comment\" : \"No comment\"}"))
                .subject(new AuditEntity(101l, "Subject", "subject@example.com"))
                .tags("TAG2")
                .build();
        AuditEvent event3 = AuditEvent.builder()
                .name("Identity name")
                .type(EventType.GROUP)
                .timestamp(new Date())
                .action(EventAction.ADD)
                .initiator(new AuditEntity(100l, "Initiator", "initiator@example.com"))
                .details(JsonUtil.parse("{\"comment\" : \"No comment\"}"))
                .subject(new AuditEntity(102l, "Subject2", "subject2@example.com"))
                .tags("TAG1", "TAG3", "TAG4")
                .build();

        tx.runInTransaction(() -> {
            dao.create(event1);
            dao.create(event2);
            dao.create(event3);

            TransactionTL.manualCommit();

            List<AuditEvent> events = dao.getAll();

            assertEquals(3, events.size());
            assertTrue(events.contains(event1));
            assertTrue(events.contains(event2));
            assertTrue(events.contains(event3));
            AuditEntity initiator = new AuditEntity(100l, "Initiator", "initiator@example.com");
            assertTrue(events.stream().allMatch((event) -> event.getInitiator().equals(initiator)));

            assertEquals(Sets.newHashSet("TAG1", "TAG2", "TAG3", "TAG4"), dao.getAllTags());
        });
    }
}
