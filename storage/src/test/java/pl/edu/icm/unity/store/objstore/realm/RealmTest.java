/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.realm;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.store.api.generic.GenericObjectsDAO;
import pl.edu.icm.unity.store.api.generic.RealmDB;
import pl.edu.icm.unity.store.objstore.AbstractObjStoreTest;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;

public class RealmTest extends AbstractObjStoreTest<AuthenticationRealm>
{
	@Autowired
	private RealmDB dao;
	
	@Override
	protected GenericObjectsDAO<AuthenticationRealm> getDAO()
	{
		return dao;
	}

	@Override
	protected AuthenticationRealm getObject(String id)
	{
		return new AuthenticationRealm(id, 
				"description", 
				3, 222, 3, 555);
	}

	@Override
	protected void mutateObject(AuthenticationRealm src)
	{
		src.setName("name-Changed");
		src.setAllowForRememberMeDays(67);
		src.setBlockAfterUnsuccessfulLogins(1);
		src.setBlockFor(777);
		src.setDescription("description2");
		src.setMaxInactivity(666);
	}

	@Override
	protected void assertAreEqual(AuthenticationRealm obj, AuthenticationRealm cmp)
	{
		assertThat(obj, is(cmp));
	}
}
