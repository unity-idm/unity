/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.endpoint;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import pl.edu.icm.unity.store.api.generic.AuthenticationFlowDB;
import pl.edu.icm.unity.store.api.generic.AuthenticatorConfigurationDB;
import pl.edu.icm.unity.store.api.generic.EndpointDB;
import pl.edu.icm.unity.store.api.generic.NamedCRUDDAOWithTS;
import pl.edu.icm.unity.store.api.generic.RealmDB;
import pl.edu.icm.unity.store.objstore.AbstractNamedWithTSTest;
import pl.edu.icm.unity.store.types.AuthenticatorConfiguration;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition.Policy;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.RememberMePolicy;
import pl.edu.icm.unity.types.endpoint.Endpoint;
import pl.edu.icm.unity.types.endpoint.EndpointConfiguration;

public class EndpointTest extends AbstractNamedWithTSTest<Endpoint>
{
	@Autowired
	private EndpointDB dao;
	
	@Autowired
	private RealmDB realmDB;

	@Autowired
	private AuthenticationFlowDB authnFlowDB;
	
	@Autowired
	private AuthenticatorConfigurationDB authnDB;
	
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
					"description", 3, 222, RememberMePolicy.disallow , 1, 555);
			realmDB.create(realm);
			
			Endpoint obj = getObject("name1");
			dao.create(obj);

			catchException(realmDB).delete("realm");
			assertThat(caughtException(), isA(IllegalArgumentException.class));
		});
	}

	@Test
	public void authenticationFlowRemovalIsBlockedByEndpoint()
	{
		tx.runInTransaction(() -> {
			AuthenticationFlowDefinition def =new AuthenticationFlowDefinition(
					"flow1", Policy.NEVER,
					Sets.newHashSet("pa"));
			authnFlowDB.create(def);
			
			Endpoint obj = getObject("name1");
			dao.create(obj);

			catchException(authnFlowDB).delete("flow1");
			assertThat(caughtException(), isA(IllegalArgumentException.class));
		});
	}
	
	@Test
	public void authenticatorRemovalIsBlockedByEndpoint()
	{
		tx.runInTransaction(() -> {
			AuthenticatorConfiguration ret = new AuthenticatorConfiguration("pa2", "", "", "", 1);
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
		EndpointConfiguration config = new EndpointConfiguration(new I18nString("displayedName"), 
				"description", 

				Lists.newArrayList("flow1", "pa2"), "configuration", "realm");
		return new Endpoint(id, "typeId", "addr", config, 1);
	}

	@Override
	protected Endpoint mutateObject(Endpoint ret)
	{
		EndpointConfiguration config = new EndpointConfiguration(new I18nString("displayedName2"), 
				"description2", 
				Lists.newArrayList("flow2", "pa2"), "configuration2", "realm2");
		return new Endpoint("changedName", "typeId2", "addr2", config, 2);
	}
}
