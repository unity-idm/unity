/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.identity;

import org.apache.logging.log4j.Logger;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import pl.edu.icm.unity.base.audit.AuditEvent;
import pl.edu.icm.unity.base.audit.AuditEventAction;
import pl.edu.icm.unity.base.audit.AuditEventType;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.identity.EntityParam;
import pl.edu.icm.unity.base.identity.EntityState;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.AuditEventManagement;
import pl.edu.icm.unity.engine.audit.AuditPublisher;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class TestIdentitiesAuditing extends DBIntegrationTestBase
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CORE, TestIdentitiesAuditing.class);

	@Autowired
	private AuditEventManagement auditManager;

	@Autowired
	private AuditPublisher auditPublisher;

	@Before
	public void prepare() throws Exception
	{
		setupMockAuthn();	
	}
	
	@Test
	public void shouldAuditAddingEntityToGroup() throws Exception
	{
		IdentityParam idParam = new IdentityParam(X500Identity.ID, "CN=golbi");
		idsMan.addEntity(idParam, "crMock", EntityState.valid);
		groupsMan.addGroup(new Group("/test2"));
		
		List<AuditEvent> allEvents = runAuditedAndWaitForEvents(() -> {
			groupsMan.addMemberFromParent("/test2", new EntityParam(idParam));
			return null;
		}, 1);
		
		assertEquals(1, allEvents.stream()
				.filter(log -> log.getType() == AuditEventType.MEMBERSHIP && log.getAction() == AuditEventAction.ADD
						&& log.getName().equals("/test2") && log.getTags().contains("Members"))
				.count());
	}

	
	@Test
	public void shouldAuditEntityCreation() throws Exception
	{
		List<AuditEvent> allEvents = runAuditedAndWaitForEvents(() -> {
			IdentityParam idParam = new IdentityParam(X500Identity.ID, "CN=golbi");
			return idsMan.addEntity(idParam, "crMock", EntityState.valid);
		}, 3);

		assertEquals(1, allEvents.stream()
				.filter(log -> log.getType() == AuditEventType.ENTITY && log.getAction() == AuditEventAction.ADD
						&& !log.getSubject().getEntityId().equals(1L))
				.count());
		assertEquals(1, allEvents.stream()
				.filter(log -> log.getType() == AuditEventType.IDENTITY && log.getAction() == AuditEventAction.ADD
						&& log.getName().equals("x500Name:cn=golbi"))
				.count());
		assertEquals(1, allEvents.stream()
				.filter(log -> log.getType() == AuditEventType.MEMBERSHIP && log.getAction() == AuditEventAction.ADD
						&& log.getName().equals("/") && log.getTags().contains("Members"))
				.count());
	}
	
	
	@Test
	public void shouldAuditDisablingEntity() throws Exception
	{
		IdentityParam idParam = new IdentityParam(X500Identity.ID, "CN=golbi");
		idsMan.addEntity(idParam, "crMock", EntityState.valid);
		groupsMan.addGroup(new Group("/test2"));
		
		List<AuditEvent> allEvents = runAuditedAndWaitForEvents(() -> {
			idsMan.setEntityStatus(new EntityParam(idParam), EntityState.disabled);
			return null;
		}, 1);
		
		assertEquals(1, allEvents.stream()
				.filter(log -> log.getType() == AuditEventType.ENTITY && log.getAction() == AuditEventAction.UPDATE
						&& !log.getSubject().getEntityId().equals(1L) && log.getDetails().toString().equals("{\"state\":\"disabled\"}"))
				.count());
	}
	
	
	@Test
	public void shouldAuditEntityRemoval() throws Exception
	{
		IdentityParam idParam = new IdentityParam(X500Identity.ID, "CN=golbi");
		idsMan.addEntity(idParam, "crMock", EntityState.valid);
		groupsMan.addGroup(new Group("/test2"));
		
		List<AuditEvent> allEvents = runAuditedAndWaitForEvents(() -> {
			idsMan.removeEntity(new EntityParam(idParam));
			return null;
		}, 1);
		
		assertEquals(1, allEvents.stream()
				.filter(log -> log.getType() == AuditEventType.ENTITY && log.getAction() == AuditEventAction.REMOVE
						&& !log.getSubject().getEntityId().equals(1L))
				.count());
	}
	
	@Test
	public void longIdentityIsSupportedWihAudit() throws Exception
	{
		ReflectionTestUtils.setField(auditPublisher, "enabled", true);
		StringBuilder id = new StringBuilder();
		for (int i=0; i<300; i++)
			id.append(i%10);

		List<AuditEvent> allEvents = runAuditedAndWaitForEvents(() -> {
			return idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, id.toString()), 
					"crMock", EntityState.valid);
		}, 3);

		assertEquals(1, allEvents.stream()
				.filter(log -> log.getType() == AuditEventType.ENTITY && log.getAction() == AuditEventAction.ADD)
				.count());
		assertEquals(1, allEvents.stream()
				.filter(log -> log.getType() == AuditEventType.IDENTITY && log.getAction() == AuditEventAction.ADD)
				.count());
		assertEquals(1, allEvents.stream()
				.filter(log -> log.getType() == AuditEventType.MEMBERSHIP && log.getAction() == AuditEventAction.ADD
						&& log.getName().equals("/") && log.getTags().contains("Members"))
				.count());
	}
	
	
	private List<AuditEvent> runAuditedAndWaitForEvents(Callable<?> operation, int expectedAuditEntries) throws Exception
	{
		try 
		{
			auditManager.enableAuditEvents();
			operation.call();
			Awaitility.with().pollInSameThread().await().atMost(10, TimeUnit.SECONDS)
					.until(() -> auditManager.getAllEvents().size() == expectedAuditEntries);
		} finally
		{
			auditManager.disableAuditEvents();
		}
		
		List<AuditEvent> allEvents = auditManager.getAllEvents();
		log.info("Logged audit: {}", allEvents.stream().map(el -> el.toString()).collect(Collectors.joining("\n")));
		return allEvents;
	}
}
