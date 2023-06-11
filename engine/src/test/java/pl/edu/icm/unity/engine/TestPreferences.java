/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.entity.EntityState;
import pl.edu.icm.unity.base.entity.Identity;
import pl.edu.icm.unity.base.entity.IdentityParam;
import pl.edu.icm.unity.stdext.identity.X500Identity;

public class TestPreferences extends DBIntegrationTestBase
{
	@Test
	public void nullIsReturnedForMissingPreference() throws Exception
	{
		setupMockAuthn();
		Identity id = idsMan.addEntity(new IdentityParam(X500Identity.ID, "cn=golbi"), 
				"crMock", EntityState.valid);
		EntityParam entity = new EntityParam(id.getEntityId());

		assertNull(preferencesMan.getPreference(entity, "foo"));
		assertNull(preferencesMan.getPreference(entity, "bar"));
	}
	
	@Test
	public void addedPreferenceIsReturned() throws Exception
	{
		setupMockAuthn();
		Identity id = idsMan.addEntity(new IdentityParam(X500Identity.ID, "cn=golbi"), 
				"crMock", EntityState.valid);
		EntityParam entity = new EntityParam(id.getEntityId());
		
		preferencesMan.setPreference(entity, "foo", "val1");
		preferencesMan.setPreference(entity, "bar", "val2");

		assertEquals("val1", preferencesMan.getPreference(entity, "foo"));
		assertEquals("val2", preferencesMan.getPreference(entity, "bar"));
	}

	@Test
	public void removedPreferenceIsNotReturned() throws Exception
	{
		setupMockAuthn();
		Identity id = idsMan.addEntity(new IdentityParam(X500Identity.ID, "cn=golbi"), 
				"crMock", EntityState.valid);
		EntityParam entity = new EntityParam(id.getEntityId());
		
		preferencesMan.setPreference(entity, "foo", "val1");
		preferencesMan.setPreference(entity, "bar", "val2");
		
		preferencesMan.removePreference(entity, "foo");
		assertNull(preferencesMan.getPreference(entity, "foo"));
		assertEquals("val2", preferencesMan.getPreference(entity, "bar"));
	}
}
