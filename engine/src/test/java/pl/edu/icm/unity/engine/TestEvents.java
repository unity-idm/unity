/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;


import static org.junit.Assert.*;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.engine.events.EventDecoratingHandler;
import pl.edu.icm.unity.engine.events.EventProcessor;
import pl.edu.icm.unity.engine.events.InvocationEventContents;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.events.Event;
import pl.edu.icm.unity.server.events.EventListener;

/**
 * Tests the core events mechanism
 * @author K. Benedyczak
 */
public class TestEvents extends DBIntegrationTestBase
{
	@Autowired
	private EventProcessor eventProcessor;
	
	@Test
	public void test() throws Exception
	{
		MockConsumer hConsumer1 = new MockConsumer();
		hConsumer1.lightweight = false;
		MockConsumer hConsumer2 = new MockConsumer();
		hConsumer2.lightweight = false;
		MockConsumer lConsumer1 = new MockConsumer();
		MockConsumer lConsumer2 = new MockConsumer();
		
		eventProcessor.addEventListener(hConsumer1);
		eventProcessor.addEventListener(hConsumer2);
		eventProcessor.addEventListener(lConsumer1);
		eventProcessor.addEventListener(lConsumer2);
		
		attrsMan.getAttributeTypes();
		Thread.sleep(800);
		assertEquals(0, hConsumer1.invocationTries);
		assertEquals(0, hConsumer2.invocationTries);
		assertEquals(0, lConsumer1.invocationTries);
		assertEquals(0, lConsumer2.invocationTries);
		assertEquals(0, eventProcessor.getPendingEventsNumber());
		
		idsMan.getIdentityTypes();
		int i=0;
		do 
		{
			Thread.sleep(500);
			assertNotEquals(40, i++);
		} while (1 != hConsumer1.invocationTries);
		
		assertEquals(1, hConsumer1.invocationTries);
		assertEquals(1, hConsumer2.invocationTries);
		assertEquals(1, lConsumer1.invocationTries);
		assertEquals(1, lConsumer2.invocationTries);
		assertEquals(1, hConsumer1.invocations);
		assertEquals(1, hConsumer2.invocations);
		assertEquals(1, lConsumer1.invocations);
		assertEquals(1, lConsumer2.invocations);
		assertEquals(2, eventProcessor.getPendingEventsNumber());

		idsMan.getIdentityTypes();
		i=0;
		do 
		{
			Thread.sleep(500);
			assertNotEquals(40, i++);
		} while (2 != hConsumer1.invocationTries);
		assertEquals(2, hConsumer1.invocationTries);
		assertEquals(2, hConsumer2.invocationTries);
		assertEquals(2, lConsumer1.invocationTries);
		assertEquals(2, lConsumer2.invocationTries);
		assertEquals(2, hConsumer1.invocations);
		assertEquals(2, hConsumer2.invocations);
		assertEquals(2, lConsumer1.invocations);
		assertEquals(2, lConsumer2.invocations);
		assertEquals(2, eventProcessor.getPendingEventsNumber());
		
		eventProcessor.removeEventListener(hConsumer2);
		eventProcessor.removeEventListener(hConsumer1);
		eventProcessor.removeEventListener(lConsumer2);
		eventProcessor.removeEventListener(lConsumer1);
	}
	
	private static class MockConsumer implements EventListener
	{
		private static int IC = 0;
		private String id = MockConsumer.class.getName() + IC++;
		private int invocations = 0;
		private int invocationTries = 0;
		private boolean lightweight = true;
		
		@Override
		public String getCategory()
		{
			return EventDecoratingHandler.CATEGORY_INVOCATION;
		}

		@Override
		public boolean isLightweight()
		{
			return lightweight;
		}

		@Override
		public boolean isWanted(Event event)
		{
			InvocationEventContents parsed = new InvocationEventContents();
			parsed.fromJson(event.getContents());
			return parsed.getInterfaceName().equals(IdentitiesManagement.class.getSimpleName());
		}

		@Override
		public boolean handleEvent(Event event)
		{
			invocationTries++;
			InvocationEventContents parsed = new InvocationEventContents();
			parsed.fromJson(event.getContents());
			if (parsed.getMethod().equals("getIdentityTypes"))
				invocations++;
			return invocations>1;
		}

		@Override
		public String getId()
		{
			return id;
		}

		@Override
		public int getMaxFailures()
		{
			return 2;
		}
		
	}
}
