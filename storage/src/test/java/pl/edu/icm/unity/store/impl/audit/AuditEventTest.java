/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.audit;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.store.api.AuditEventDAO;
import pl.edu.icm.unity.store.impl.AbstractBasicDAOTest;
import pl.edu.icm.unity.store.tx.TransactionTL;
import pl.edu.icm.unity.types.basic.AuditEvent;
import pl.edu.icm.unity.types.basic.AuditEvent.AuditEntity;
import pl.edu.icm.unity.types.basic.AuditEvent.EventAction;
import pl.edu.icm.unity.types.basic.AuditEvent.EventType;

import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AuditEventTest extends AbstractBasicDAOTest<AuditEvent>
{
	@Autowired
	private AuditEventDAO dao;

	@Override
	protected AuditEventDAO getDAO()
	{
		return dao;
	}

	@Override
	protected AuditEvent getObject(String id)
	{
		AuditEvent event = new AuditEvent("name",
				EventType.IDENTITY,
				new Date(),
				EventAction.ADD,
				JsonUtil.parse("{\"comment\" : \"No comment\"}"),
				new AuditEntity(101l, "Subject", "subject@example.com"),
				new AuditEntity(100l, "Initiator", "initiator@example.com"),
				"TAG1", "TAG2");
		return event;
	}

	@Override
	protected AuditEvent mutateObject(AuditEvent src)
	{
		AuditEvent event = new AuditEvent("name2",
				EventType.IDENTITY,
				new Date(),
				EventAction.UPDATE,
				JsonUtil.parse("{\"comment\" : \"No new comment\"}"),
				new AuditEntity(102l, "Subject2", "subject2@example.com"),
				src.getInitiator(),
				"TAG1", "TAG3");
		return event;
	}

	@Test
	public void checkNullableFields() {
		AuditEvent event = new AuditEvent("Group name",
				EventType.GROUP,
				new Date(),
				EventAction.ADD,
				null,
				null,
				new AuditEntity(100l, "Initiator", "initiator@example.com"));
		tx.runInTransaction(() -> {
			long id = dao.create(event);
			TransactionTL.manualCommit();
			AuditEvent eventFromDB = dao.getByKey(id);
			assertEquals(event, eventFromDB);
			assertTrue(eventFromDB.getJsonDetails() == null);
			assertTrue(eventFromDB.getSubject() == null);
			assertTrue(eventFromDB.getTags().isEmpty());
		});
	}

	@Test
	public void getAllEvents() {
		AuditEvent event1 = new AuditEvent("Identity name",
				EventType.IDENTITY,
				new Date(),
				EventAction.ADD,
				JsonUtil.parse("{\"comment\" : \"No comment\"}"),
				new AuditEntity(101l, "Subject", "subject@example.com"),
				new AuditEntity(100l, "Initiator", "initiator@example.com"),
				"TAG1", "TAG2");
		AuditEvent event2 = new AuditEvent("Identity name",
				EventType.IDENTITY,
				new Date(),
				EventAction.UPDATE,
				JsonUtil.parse("{\"comment\" : \"No comment\"}"),
				new AuditEntity(101l, "Subject", "subject@example.com"),
				new AuditEntity(100l, "Initiator", "initiator@example.com"),
				"TAG2");
		AuditEvent event3 = new AuditEvent("Group name",
				EventType.GROUP,
				new Date(),
				EventAction.ADD,
				JsonUtil.parse("{\"comment\" : \"No comment\"}"),
				new AuditEntity(102l, "Subject2", "subject2@example.com"),
				new AuditEntity(100l, "Initiator", "initiator@example.com"),
				"TAG1", "TAG3", "TAG4");

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
			assertEquals(4, dao.getAllTags().size());
		});
	}
}
