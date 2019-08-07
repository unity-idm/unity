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
import pl.edu.icm.unity.types.basic.audit.AuditEntity;
import pl.edu.icm.unity.types.basic.audit.AuditEvent;
import pl.edu.icm.unity.types.basic.audit.AuditEventAction;
import pl.edu.icm.unity.types.basic.audit.AuditEventType;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

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
				.type(AuditEventType.IDENTITY)
				.timestamp(new Date())
				.action(AuditEventAction.ADD)
				.details(JsonUtil.parse("{\"comment\" : \"No comment\"}"))
				.subject(new AuditEntity(101L, "Subject", "subject@example.com"))
				.initiator(new AuditEntity(100L, "Initiator", "initiator@example.com"))
				.tags("TAG1", "TAG2")
				.build();
	}

	@Override
	protected AuditEvent mutateObject(AuditEvent src)
	{
		return AuditEvent.builder()
				.name("name2")
				.type(AuditEventType.IDENTITY)
				.timestamp(new Date())
				.action(AuditEventAction.UPDATE)
				.details(JsonUtil.parse("{\"comment\" : \"No new comment\"}"))
				.subject(new AuditEntity(102L, "Subject2", "subject2@example.com"))
				.initiator(src.getInitiator())
				.tags("TAG2", "TAG3")
				.build();
	}

	@Test
	public void shouldStoreEventWithNulls()
	{
		// given
		AuditEvent event = AuditEvent.builder()
				.name("name2")
				.type(AuditEventType.IDENTITY)
				.timestamp(new Date())
				.action(AuditEventAction.UPDATE)
				.initiator(new AuditEntity(100L, "Initiator", "initiator@example.com"))
				.build();
		tx.runInTransaction(() -> {
			// when
			long id = dao.create(event);
			TransactionTL.manualCommit();

			// than
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
		// given
		AuditEvent event1 = AuditEvent.builder()
				.name("Identity name")
				.type(AuditEventType.IDENTITY)
				.timestamp(new Date())
				.action(AuditEventAction.ADD)
				.initiator(new AuditEntity(100L, "Initiator", "initiator@example.com"))
				.details(JsonUtil.parse("{\"comment\" : \"No comment\"}"))
				.subject(new AuditEntity(101L, "Subject", "subject@example.com"))
				.tags("TAG1")
				.build();
		AuditEvent event2 = AuditEvent.builder()
				.name("Identity name")
				.type(AuditEventType.IDENTITY)
				.timestamp(new Date())
				.action(AuditEventAction.UPDATE)
				.initiator(new AuditEntity(100L, "Initiator", "initiator@example.com"))
				.details(JsonUtil.parse("{\"comment\" : \"No comment\"}"))
				.subject(new AuditEntity(101L, "Subject", "subject@example.com"))
				.tags("TAG2")
				.build();
		AuditEvent event3 = AuditEvent.builder()
				.name("Identity name")
				.type(AuditEventType.GROUP)
				.timestamp(new Date())
				.action(AuditEventAction.ADD)
				.initiator(new AuditEntity(100L, "Initiator", "initiator@example.com"))
				.details(JsonUtil.parse("{\"comment\" : \"No comment\"}"))
				.subject(new AuditEntity(102L, "Subject2", "subject2@example.com"))
				.tags("TAG1", "TAG3", "TAG4")
				.build();


		tx.runInTransaction(() -> {
			// when
			dao.create(event1);
			dao.create(event2);
			dao.create(event3);

			TransactionTL.manualCommit();

			// than
			List<AuditEvent> events = dao.getAll();
			assertEquals(3, events.size());
			assertTrue(events.contains(event1));
			assertTrue(events.contains(event2));
			assertTrue(events.contains(event3));
			AuditEntity initiator = new AuditEntity(100L, "Initiator", "initiator@example.com");
			assertTrue(events.stream().allMatch((event) -> event.getInitiator().equals(initiator)));
			Set<String> allTags = dao.getAllTags();
			assertTrue(allTags.containsAll(Arrays.asList("TAG1", "TAG2", "TAG3", "TAG4")));
		});
	}

	@Test
	public void shouldIgnoreDuplicateTags()
	{
		// given
		AuditEvent event1 = getObject("");
		AuditEvent event2 = getObject("");

		tx.runInTransaction(() -> {
			//when
			dao.create(event1);
			TransactionTL.manualCommit();
			// make sure cache is empty
			tagDAO.invalidateCache();
			dao.create(event2);
			TransactionTL.manualCommit();

			// than
			List<AuditEvent> events = dao.getAll();
			assertEquals(2, events.size());
			assertTrue(events.contains(event1));
			assertTrue(events.contains(event2));
			Set<String> allTags = dao.getAllTags();
			assertTrue(allTags.containsAll(Arrays.asList("TAG1", "TAG2")));
		});
	}

	@Test
	public void shouldInsertAuditEntitiesWithNulls()
	{
		// given
		AuditEvent event1 = AuditEvent.builder()
				.name("Identity name")
				.type(AuditEventType.IDENTITY)
				.timestamp(new Date())
				.action(AuditEventAction.ADD)
				.initiator(new AuditEntity(100L, null, null))
				.details(JsonUtil.parse("{\"comment\" : \"No comment\"}"))
				.subject(new AuditEntity(101L, null, null))
				.tags("TAG1")
				.build();
		AuditEvent event2 = AuditEvent.builder()
				.name("Identity name")
				.type(AuditEventType.IDENTITY)
				.timestamp(new Date())
				.action(AuditEventAction.UPDATE)
				.initiator(new AuditEntity(100L, null, null))
				.details(JsonUtil.parse("{\"comment\" : \"No comment\"}"))
				.subject(new AuditEntity(101L, null, null))
				.tags("TAG2")
				.build();


		tx.runInTransaction(() -> {
			// when
			dao.create(event1);
			dao.create(event2);

			TransactionTL.manualCommit();

			// than
			List<AuditEvent> events = dao.getAll();
			assertEquals(2, events.size());
			assertTrue(events.contains(event1));
			assertTrue(events.contains(event2));
			AuditEntity initiator = new AuditEntity(100L, null, null);
			assertTrue(events.stream().allMatch((event) -> event.getInitiator().equals(initiator)));
			Set<String> allTags = dao.getAllTags();
			assertTrue(allTags.containsAll(Arrays.asList("TAG1", "TAG2")));
		});
	}

	@Test
	public void shouldDifferentiateAuditEntitiesWithNulls()
	{
		AuditEntity subject1 = new AuditEntity(101L, null, null);
		AuditEntity subject2 = new AuditEntity(101L, "Subject", null);
		AuditEntity subject3 = new AuditEntity(101L, null, "subject@example.com");
		AuditEntity subject4 = new AuditEntity(101L, "Subject", "subject@example.com");
		// given
		AuditEvent event1 = AuditEvent.builder()
				.name("Identity name")
				.type(AuditEventType.IDENTITY)
				.timestamp(new Date())
				.action(AuditEventAction.ADD)
				.initiator(new AuditEntity(100L, "Initiator", "initiator@example.com"))
				.details(JsonUtil.parse("{\"comment\" : \"No comment\"}"))
				.subject(subject1)
				.tags("TAG1")
				.build();
		AuditEvent event2 = AuditEvent.builder()
				.name("Identity name")
				.type(AuditEventType.IDENTITY)
				.timestamp(new Date())
				.action(AuditEventAction.UPDATE)
				.initiator(new AuditEntity(100L, "Initiator", "initiator@example.com"))
				.details(JsonUtil.parse("{\"comment\" : \"No comment\"}"))
				.subject(subject2)
				.tags("TAG2")
				.build();
		AuditEvent event3 = AuditEvent.builder()
				.name("Identity name")
				.type(AuditEventType.GROUP)
				.timestamp(new Date())
				.action(AuditEventAction.ADD)
				.initiator(new AuditEntity(100L, "Initiator", "initiator@example.com"))
				.details(JsonUtil.parse("{\"comment\" : \"No comment\"}"))
				.subject(subject3)
				.tags("TAG1", "TAG3", "TAG4")
				.build();
		AuditEvent event4 = AuditEvent.builder()
				.name("Identity name")
				.type(AuditEventType.GROUP)
				.timestamp(new Date())
				.action(AuditEventAction.ADD)
				.initiator(new AuditEntity(100L, "Initiator", "initiator@example.com"))
				.details(JsonUtil.parse("{\"comment\" : \"No comment\"}"))
				.subject(subject4)
				.tags("TAG1", "TAG3", "TAG4")
				.build();

		tx.runInTransaction(() -> {
			// when
			dao.create(event1);
			dao.create(event2);
			dao.create(event3);
			dao.create(event4);

			TransactionTL.manualCommit();

			// than
			List<AuditEvent> events = dao.getAll();
			assertEquals(4, events.size());
			assertTrue(events.contains(event1));
			assertTrue(events.contains(event2));
			assertTrue(events.contains(event3));
			assertTrue(events.contains(event4));
			AuditEntity initiator = new AuditEntity(100L, "Initiator", "initiator@example.com");
			assertTrue(events.stream().allMatch((event) -> event.getInitiator().equals(initiator)));
			assertEquals(1, events.stream().filter((event) -> event.getSubject().equals(subject1)).count());
			assertEquals(1, events.stream().filter((event) -> event.getSubject().equals(subject2)).count());
			assertEquals(1, events.stream().filter((event) -> event.getSubject().equals(subject3)).count());
			assertEquals(1, events.stream().filter((event) -> event.getSubject().equals(subject4)).count());
			Set<String> allTags = dao.getAllTags();
			assertTrue(allTags.containsAll(Arrays.asList("TAG1", "TAG2", "TAG3", "TAG4")));
		});
	}

	@Override
	public void shouldReturnUpdatedByKey()
	{
		// Update not supported for AuditEvents
	}

	@Override
	public void shouldFailOnUpdatingAbsent()
	{
		// Update not supported for AuditEvents
	}
}
