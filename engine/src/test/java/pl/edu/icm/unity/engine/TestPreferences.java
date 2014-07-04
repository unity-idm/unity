/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.junit.Assert.*;

import org.junit.Test;

import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;

public class TestPreferences extends DBIntegrationTestBase
{
	@Test
	public void testPreferences() throws Exception
	{
		setupMockAuthn();
		Identity id = idsMan.addEntity(new IdentityParam(X500Identity.ID, "cn=golbi"), 
				"crMock", EntityState.valid, false);
		EntityParam entity = new EntityParam(id.getEntityId());
		assertNull(preferencesMan.getPreference(entity, "foo"));
		assertNull(preferencesMan.getPreference(entity, "foo"));
		
		preferencesMan.setPreference(entity, "foo", "val1");
		assertEquals("val1", preferencesMan.getPreference(entity, "foo"));
		preferencesMan.setPreference(entity, "bar", "val2");
		assertEquals("val1", preferencesMan.getPreference(entity, "foo"));
		assertEquals("val2", preferencesMan.getPreference(entity, "bar"));
		
		preferencesMan.removePreference(entity, "foo");
		assertNull(preferencesMan.getPreference(entity, "foo"));
		assertEquals("val2", preferencesMan.getPreference(entity, "bar"));

		preferencesMan.removePreference(entity, "bar");
		assertNull(preferencesMan.getPreference(entity, "bar"));
	}
}
