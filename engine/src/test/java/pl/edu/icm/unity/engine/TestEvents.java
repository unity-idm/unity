/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;


import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.event.Event;
import pl.edu.icm.unity.base.event.PersistableEvent;
import pl.edu.icm.unity.engine.api.IdentityTypesManagement;
import pl.edu.icm.unity.engine.api.event.EventListener;
import pl.edu.icm.unity.engine.events.EventProcessor;
import pl.edu.icm.unity.engine.events.EventProducingAspect;
import pl.edu.icm.unity.engine.events.InvocationEventContents;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Tests the core events mechanism
 * @author K. Benedyczak
 */
@Ignore
public class TestEvents extends DBIntegrationTestBase
{
	@Autowired
	private EventProcessor eventProcessor;

	@Autowired
	EventProducingAspect eventProducingAspect;

	private MockConsumer hConsumer1 = new MockConsumer();
	private MockConsumer hConsumer2 = new MockConsumer();
	private MockConsumer lConsumer1 = new MockConsumer();
	private MockConsumer lConsumer2 = new MockConsumer();

	@Test
	public void test() throws Exception
	{
		hConsumer1.lightweight = false;
		hConsumer2.lightweight = false;
		
		eventProcessor.addEventListener(hConsumer1);
		eventProcessor.addEventListener(hConsumer2);
		eventProcessor.addEventListener(lConsumer1);
		eventProcessor.addEventListener(lConsumer2);
		
		aTypeMan.getAttributeTypes();
		Thread.sleep(800);
		testInvocationTries(0, true);
		assertEquals(0, eventProcessor.getPendingEventsNumber());
		
		idTypeMan.getIdentityTypes();
		int i=0;
		do 
		{
			Thread.sleep(500);
			assertNotEquals(10, i++);
		} while (!testInvocationTries(1, false) || !testInvocations(1, false));
		
		testInvocationTries(1, true);
		testInvocations(1, true);
		assertEquals(2, eventProcessor.getPendingEventsNumber());

		idTypeMan.getIdentityTypes();
		i=0;
		do 
		{
			Thread.sleep(500);
			assertNotEquals(40, i++);
		} while (!testInvocationTries(2, false) || !testInvocations(2, false));

		testInvocationTries(2, true);
		testInvocations(2, true);
		assertEquals(2, eventProcessor.getPendingEventsNumber());
		
		eventProcessor.removeEventListener(hConsumer2);
		eventProcessor.removeEventListener(hConsumer1);
		eventProcessor.removeEventListener(lConsumer2);
		eventProcessor.removeEventListener(lConsumer1);
	}
	
	private boolean testInvocationTries(int expected, boolean fail)
	{
		if (fail)
		{
			assertEquals(expected, hConsumer1.invocationTries);
			assertEquals(expected, hConsumer2.invocationTries);
			assertEquals(expected, lConsumer1.invocationTries);
			assertEquals(expected, lConsumer2.invocationTries);
			return true;
		} else
		{
			return expected == hConsumer1.invocationTries && expected == hConsumer2.invocationTries &&
					expected == lConsumer1.invocationTries && expected == lConsumer2.invocationTries;
		}
	}

	private boolean testInvocations(int expected, boolean fail)
	{
		if (fail)
		{
			assertEquals(expected, hConsumer1.invocations);
			assertEquals(expected, hConsumer2.invocations);
			assertEquals(expected, lConsumer1.invocations);
			assertEquals(expected, lConsumer2.invocations);
			return true;
		} else
		{
			return expected == hConsumer1.invocations && expected == hConsumer2.invocations &&
					expected == lConsumer1.invocations && expected == lConsumer2.invocations;
		}
	}
	
	private static class MockConsumer implements EventListener
	{
		private static int IC = 0;
		private String id = MockConsumer.class.getName() + IC++;
		private int invocations = 0;
		private int invocationTries = 0;
		private boolean lightweight = true;
		
		@Override
		public boolean isLightweight()
		{
			return lightweight;
		}

		@Override
		public boolean isWanted(Event abstractEvent)
		{
			if (!(abstractEvent instanceof PersistableEvent))
				return false;
			PersistableEvent event = (PersistableEvent)abstractEvent;
			InvocationEventContents parsed = new InvocationEventContents();
			parsed.fromJson(event.getContents());
			return parsed.getInterfaceName().equals(IdentityTypesManagement.class.getSimpleName());
		}

		@Override
		public boolean handleEvent(Event abstractEvent)
		{
			PersistableEvent event = (PersistableEvent)abstractEvent;
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

		@Override
		public boolean isAsync(Event event)
		{
			return false;
		}
	}
}
