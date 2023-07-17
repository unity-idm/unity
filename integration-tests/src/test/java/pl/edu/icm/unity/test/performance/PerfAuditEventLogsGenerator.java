/******************************************************************************
 * Copyright (c) 2019, T-Mobile US.
 * <p>
 * All Rights Reserved
 * <p>
 * This is unpublished proprietary source code of T-Mobile US.
 * <p>
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 *******************************************************************************/
package pl.edu.icm.unity.test.performance;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.ImmutableMap;

import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.audit.AuditEntity;
import pl.edu.icm.unity.base.audit.AuditEvent;
import pl.edu.icm.unity.base.audit.AuditEventAction;
import pl.edu.icm.unity.base.audit.AuditEventType;
import pl.edu.icm.unity.engine.UnityIntegrationTest;
import pl.edu.icm.unity.store.api.AuditEventDAO;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;

@ExtendWith(SpringExtension.class)
@UnityIntegrationTest
public class PerfAuditEventLogsGenerator
{
	long entityId = 10_000_000;
	long groupId = 50_000_000;
	long time = 1483228800000L; // 2018-01-01 00:00:00 GMT

	@Autowired
	private AuditEventDAO dao;

	@Autowired
	private TransactionalRunner tx;

	// allow to create big amount of AuditEvents
	// @Test
	public void createEventsInLoop()
	{
		for (int i=0; i<100_000; i++)
		{
			createEvents(); // creates 10 AuditEvents
			entityId++;
			groupId++;
			time += 60_000; // 1 min
			if (i % 100 == 0)
			{
				System.out.println("Generated: " + i * 10);
			}
		}
	}

	//@Test
	public void create10AuditLogs()
	{
		//given
		int initialAmount = tx.runInTransactionRet(() -> dao.getAll()).size();

		//when
		createEvents();

		//then
		assertEquals(initialAmount + 10, tx.runInTransactionRet(() -> dao.getAll()).size());
	}

	private void createEvents()
	{
		List<AuditEvent> events = new ArrayList<>();

		events.add(AuditEvent.builder()
				.type(AuditEventType.ENTITY)
				.action(AuditEventAction.ADD)
				.timestamp(new Date(time))
				.name("")
				.subject(new AuditEntity(entityId, null, null))
				.initiator(new AuditEntity(0L, "System", null))
				.tags("Users")
				.build());
		events.add(AuditEvent.builder()
				.type(AuditEventType.IDENTITY)
				.action(AuditEventAction.ADD)
				.timestamp(new Date(time + 1000))
				.name("dn:user" + entityId)
				.subject(new AuditEntity(entityId, "User " + entityId, null))
				.initiator(new AuditEntity(0L, "System", null))
				.tags("Users")
				.build());
		events.add(AuditEvent.builder()
				.type(AuditEventType.CREDENTIALS)
				.action(AuditEventAction.ADD)
				.timestamp(new Date(time + 2000))
				.name("sys:Credential:sys:password")
				.subject(new AuditEntity(entityId, "User " + entityId, null))
				.initiator(new AuditEntity(0L, "System", null))
				.tags("Authn")
				.build());
		events.add(AuditEvent.builder()
				.type(AuditEventType.GROUP)
				.action(AuditEventAction.ADD)
				.timestamp(new Date(time + 3000))
				.name("/" + groupId)
				.initiator(new AuditEntity(0L, "System", null))
				.tags("Groups")
				.build());
		events.add(AuditEvent.builder()
				.type(AuditEventType.GROUP)
				.action(AuditEventAction.UPDATE)
				.timestamp(new Date(time + 4000))
				.name("/")
				.subject(new AuditEntity(entityId, "User " + entityId, null))
				.initiator(new AuditEntity(0L, "System", null))
				.details(Constants.MAPPER.valueToTree(ImmutableMap.of("action", "add")))
				.tags("Groups", "Members")
				.build());
		events.add(AuditEvent.builder()
				.type(AuditEventType.GROUP)
				.action(AuditEventAction.UPDATE)
				.timestamp(new Date(time + 5000))
				.name("/" + groupId)
				.subject(new AuditEntity(entityId, "User " + entityId, null))
				.initiator(new AuditEntity(0L, "System", null))
				.details(Constants.MAPPER.valueToTree(ImmutableMap.of("action", "add")))
				.tags("Groups", "Members")
				.build());
		events.add(AuditEvent.builder()
				.type(AuditEventType.GROUP)
				.action(AuditEventAction.UPDATE)
				.timestamp(new Date(time + 6000))
				.name("/" + groupId)
				.subject(new AuditEntity(entityId, "User " + entityId, null))
				.initiator(new AuditEntity(0L, "System", null))
				.details(Constants.MAPPER.valueToTree(ImmutableMap.of("action", "remove")))
				.tags("Groups", "Members")
				.build());
		events.add(AuditEvent.builder()
				.type(AuditEventType.GROUP)
				.action(AuditEventAction.UPDATE)
				.timestamp(new Date(time + 7000))
				.name("/")
				.subject(new AuditEntity(entityId, "User " + entityId, null))
				.initiator(new AuditEntity(0L, "System", null))
				.details(Constants.MAPPER.valueToTree(ImmutableMap.of("action", "remove")))
				.tags("Groups", "Members")
				.build());
		events.add(AuditEvent.builder()
				.type(AuditEventType.ENTITY)
				.action(AuditEventAction.REMOVE)
				.timestamp(new Date(time + 8000))
				.name("")
				.subject(new AuditEntity(entityId, "User " + entityId, null))
				.initiator(new AuditEntity(0L, "System", null))
				.tags("Users")
				.build());
		events.add(AuditEvent.builder()
				.type(AuditEventType.GROUP)
				.action(AuditEventAction.REMOVE)
				.timestamp(new Date(time + 9000))
				.name("/" + groupId)
				.initiator(new AuditEntity(0L, "System", null))
				.tags("Groups")
				.build());

		tx.runInTransaction(() -> {
			events.forEach(e -> dao.create(e));
		});
	}
}
