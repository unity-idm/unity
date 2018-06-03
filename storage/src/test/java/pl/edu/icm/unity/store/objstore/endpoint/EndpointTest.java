/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.endpoint;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.store.api.generic.AuthenticatorInstanceDB;
import pl.edu.icm.unity.store.api.generic.EndpointDB;
import pl.edu.icm.unity.store.api.generic.NamedCRUDDAOWithTS;
import pl.edu.icm.unity.store.api.generic.RealmDB;
import pl.edu.icm.unity.store.objstore.AbstractNamedWithTSTest;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticationOptionDescription;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.AuthenticatorInstance;
import pl.edu.icm.unity.types.endpoint.Endpoint;
import pl.edu.icm.unity.types.endpoint.EndpointConfiguration;

public class EndpointTest extends AbstractNamedWithTSTest<Endpoint>
{
	@Autowired
	private EndpointDB dao;
	
	@Autowired
	private RealmDB realmDB;

	@Autowired
	private AuthenticatorInstanceDB authnDB;
	
	@Override
	protected NamedCRUDDAOWithTS<Endpoint> getDAO()
	{
		return dao;
	}

	@Test
	public void realmRemovalIsBlockedByEndpoint()
	{
		tx.runInTransaction(() -> {
			AuthenticationRealm realm = new AuthenticationRealm("realm", 
					"description", 3, 222, 3, 555);
			realmDB.create(realm);
			
			Endpoint obj = getObject("name1");
			dao.create(obj);

			catchException(realmDB).delete("realm");
			assertThat(caughtException(), isA(IllegalArgumentException.class));
		});
	}

	@Test
	public void authenticatorRemovalIsBlockedByEndpoint()
	{
		tx.runInTransaction(() -> {
			AuthenticatorInstance ret = new AuthenticatorInstance();
			ret.setId("pa2");
			ret.setVerificatorConfiguration("vCfg");
			authnDB.create(ret);
			
			Endpoint obj = getObject("name1");
			dao.create(obj);

			catchException(authnDB).delete("pa2");
			assertThat(caughtException(), isA(IllegalArgumentException.class));
		});
	}

	@Override
	protected Endpoint getObject(String id)
	{
		List<AuthenticationOptionDescription> authn = Lists.newArrayList(
				new AuthenticationOptionDescription("pa", "sa"),
				new AuthenticationOptionDescription("pa2"));
		EndpointConfiguration config = new EndpointConfiguration(new I18nString("displayedName"), 
				"description", 
				authn, "configuration", "realm");
		return new Endpoint(id, "typeId", "addr", config, 1);
	}

	@Override
	protected Endpoint mutateObject(Endpoint ret)
	{
		List<AuthenticationOptionDescription> authn = Lists.newArrayList(
				new AuthenticationOptionDescription("pa4"));
		EndpointConfiguration config = new EndpointConfiguration(new I18nString("displayedName2"), 
				"description2", 
				authn, "configuration2", "realm2");
		return new Endpoint("changedName", "typeId2", "addr2", config, 2);
	}
}
