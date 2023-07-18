/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.entity.EntityState;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.identity.IdentityParam;
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

		assertThat(preferencesMan.getPreference(entity, "foo")).isNull();
		assertThat(preferencesMan.getPreference(entity, "bar")).isNull();
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

		assertThat(preferencesMan.getPreference(entity, "foo")).isEqualTo("val1");
		assertThat(preferencesMan.getPreference(entity, "bar")).isEqualTo("val2");
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
		assertThat(preferencesMan.getPreference(entity, "foo")).isNull();
		assertThat(preferencesMan.getPreference(entity, "bar")).isEqualTo("val2");
	}
}
