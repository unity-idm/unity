/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.realm;

import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.store.api.generic.NamedCRUDDAOWithTS;
import pl.edu.icm.unity.store.api.generic.RealmDB;
import pl.edu.icm.unity.store.objstore.AbstractNamedWithTSTest;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.RememberMePolicy;

public class RealmTest extends AbstractNamedWithTSTest<AuthenticationRealm>
{
	@Autowired
	private RealmDB dao;
	
	@Override
	protected NamedCRUDDAOWithTS<AuthenticationRealm> getDAO()
	{
		return dao;
	}

	@Override
	protected AuthenticationRealm getObject(String id)
	{
		return new AuthenticationRealm(id, 
				"description", 
				3, 222, RememberMePolicy.disallow , 1, 555);
	}

	@Override
	protected AuthenticationRealm mutateObject(AuthenticationRealm src)
	{
		src.setName("name-Changed");
		src.setRememberMePolicy(RememberMePolicy.allowForWholeAuthn);
		src.setAllowForRememberMeDays(67);
		src.setBlockAfterUnsuccessfulLogins(1);
		src.setBlockFor(777);
		src.setDescription("description2");
		src.setMaxInactivity(666);
		return src;
	}
}
