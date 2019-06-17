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
import pl.edu.icm.unity.types.basic.audit.AuditEvent;
import pl.edu.icm.unity.types.basic.audit.EventAction;
import pl.edu.icm.unity.types.basic.audit.EventType;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class AuditManagerTest extends DBIntegrationTestBase
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, AuditManagerTest.class);

	@Autowired
	private AuditManager auditManager;

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
		long initialLogSize = auditManager.getAllEvents().size();

		// when
		auditManager.fireEvent(EventType.ENTITY,
				EventAction.UPDATE,
				Long.toString(1L),
				1L,
				null,
				"Users");

		//than
		await().atMost(10, TimeUnit.SECONDS).until(() -> (auditManager.getAllEvents().size() == initialLogSize + 1));

		List<AuditEvent> allEvents = auditManager.getAllEvents();
		AuditEvent lastEvent = allEvents.get(allEvents.size() - 1);
		assertEquals(EventType.ENTITY, lastEvent.getType());
		assertEquals(EventAction.UPDATE, lastEvent.getAction());
		assertEquals(1, (long) lastEvent.getInitiator().getEntityId());
		assertEquals(1, (long) lastEvent.getSubject().getEntityId());
		assertEquals(1, lastEvent.getTags().size());
		assertTrue(lastEvent.getTags().contains("Users"));
	}
}