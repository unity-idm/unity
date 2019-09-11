/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.audit;

import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.types.basic.audit.AuditEvent;
import pl.edu.icm.unity.types.basic.audit.AuditEventAction;
import pl.edu.icm.unity.types.basic.audit.AuditEventType;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static pl.edu.icm.unity.types.basic.audit.AuditEventTag.USERS;

public class AuditManagerTest extends DBIntegrationTestBase
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, AuditManagerTest.class);

	@Autowired
	private AuditManager auditManager;

	@Autowired
	private TransactionalRunner tx;

	@Before
	public void setup()
	{
		InvocationContext invContext = new InvocationContext(null, null, null);
		invContext.setLoginSession(new LoginSession("1", null, null, 100, 1L, null, null, null, null));
		InvocationContext.setCurrent(invContext);
	}

	@Test
	public void shouldStoreAndRetrieveAuditEvent()
	{
		// given
		int initialLogSize = auditManager.getAllEvents().size();

		// when
		tx.runInTransaction(() -> auditManager.log(AuditEventTrigger.builder()
			.type(AuditEventType.ENTITY)
			.action(AuditEventAction.UPDATE)
			.name("")
			.subject(1L)
			.tags(USERS)));

		//than
		await().atMost(10, TimeUnit.SECONDS).until(() -> (auditManager.getAllEvents().size() == initialLogSize + 1));

		List<AuditEvent> allEvents = auditManager.getAllEvents();
		AuditEvent lastEvent = allEvents.get(allEvents.size() - 1);
		assertEquals(AuditEventType.ENTITY, lastEvent.getType());
		assertEquals(AuditEventAction.UPDATE, lastEvent.getAction());
		assertEquals(1, (long) lastEvent.getInitiator().getEntityId());
		assertEquals(1, (long) lastEvent.getSubject().getEntityId());
		assertEquals(1, lastEvent.getTags().size());
		assertTrue(lastEvent.getTags().contains("Users"));
	}

	@Test
	public void shouldGetAllTags()
	{
		// given
		int initialLogSize = auditManager.getAllEvents().size();

		// when
		tx.runInTransaction(() -> auditManager.log(AuditEventTrigger.builder()
				.type(AuditEventType.ENTITY)
				.action(AuditEventAction.UPDATE)
				.name("")
				.subject(1L)
				.tags("Users", "Members", "Groups", "Authn", "Test tag")));

		//than
		await().atMost(10, TimeUnit.SECONDS).until(() -> (auditManager.getAllEvents().size() == initialLogSize + 1));
		Set<String> allTags = auditManager.getAllTags();
		System.out.println("All: " + allTags);
		assertEquals(5, allTags.size());
		assertTrue(allTags.containsAll(Arrays.asList("Users", "Members", "Groups", "Authn", "Test tag")));
	}

	@Test
	public void shouldGetEventsForDefinedPeriodAndLimit()
	{
		// given
		Date nowPlusHour = new Date(System.currentTimeMillis() + 3600 * 1000);
		Date nowPlusDay = new Date(nowPlusHour.getTime() + (23 * 3600 * 1000));
		Date nowPlus2Days = new Date(nowPlusHour.getTime() + (47 * 3600 * 1000));

		// when
		tx.runInTransaction(() -> {
			auditManager.log(AuditEventTrigger.builder()
					.type(AuditEventType.ENTITY)
					.action(AuditEventAction.UPDATE)
					.timestamp(nowPlusHour)
					.name("")
					.subject(1L)
					.tags(USERS));
			auditManager.log(AuditEventTrigger.builder()
					.type(AuditEventType.ENTITY)
					.action(AuditEventAction.UPDATE)
					.timestamp(nowPlusDay)
					.name("")
					.subject(2L)
					.tags(USERS));
			auditManager.log(AuditEventTrigger.builder()
					.type(AuditEventType.ENTITY)
					.action(AuditEventAction.UPDATE)
					.timestamp(nowPlus2Days)
					.name("")
					.subject(3L)
					.tags(USERS));
		});

		//than
		await().atMost(10, TimeUnit.SECONDS).until(() -> (auditManager.getAuditEvents(nowPlusHour, null, 3).size() == 3));

		assertTrue(auditManager.getAuditEvents(null, null, 10).size() > 3);
		assertEquals(3, auditManager.getAuditEvents(nowPlusHour, null, 10).size());
		assertEquals(2, auditManager.getAuditEvents(nowPlusDay, null, 10).size());
		assertEquals(2, auditManager.getAuditEvents(nowPlusHour, nowPlusDay, 10).size());
		assertEquals(3, auditManager.getAuditEvents(nowPlusHour, nowPlus2Days, 10).size());
		assertEquals(3, auditManager.getAuditEvents(nowPlusHour, null, 3).size());
		assertEquals(1, auditManager.getAuditEvents(nowPlusHour, null, 1).size());
	}
}