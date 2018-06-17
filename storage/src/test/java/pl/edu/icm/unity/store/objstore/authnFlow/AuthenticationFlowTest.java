/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.authnFlow;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import pl.edu.icm.unity.store.api.generic.AuthenticationFlowDB;
import pl.edu.icm.unity.store.api.generic.AuthenticatorInstanceDB;
import pl.edu.icm.unity.store.api.generic.NamedCRUDDAOWithTS;
import pl.edu.icm.unity.store.objstore.AbstractNamedWithTSTest;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition.Policy;
import pl.edu.icm.unity.types.authn.AuthenticatorInstance;

public class AuthenticationFlowTest extends AbstractNamedWithTSTest<AuthenticationFlowDefinition>
{
	@Autowired
	private AuthenticationFlowDB dao;
	
	@Autowired
	private AuthenticatorInstanceDB authDB;
	
	@Override
	protected NamedCRUDDAOWithTS<AuthenticationFlowDefinition> getDAO()
	{
		return dao;
	}

	@Test
	public void authenticatorRemovalIsBlockedByAuthenticationFlow()
	{
		tx.runInTransaction(() -> {
			
			AuthenticatorInstance ret = new AuthenticatorInstance();
			ret.setId("pass");
			ret.setLocalCredentialName("pass");
			ret.setRetrievalConfiguration("");
			authDB.create(ret);
			
			AuthenticationFlowDefinition obj = getObject("name1");
			dao.create(obj);

			catchException(authDB).delete("pass");
			assertThat(caughtException(), isA(IllegalArgumentException.class));
		});
	}

	@Override
	protected AuthenticationFlowDefinition getObject(String id)
	{

		return new AuthenticationFlowDefinition(
				id , Policy.REQUIRE,
				Sets.newHashSet("pass"), Lists.newArrayList("cert"));
	}

	@Override
	protected AuthenticationFlowDefinition mutateObject(AuthenticationFlowDefinition ret)
	{
		ret.setFirstFactorAuthenticators(Sets.newHashSet("pass2"));
		ret.setPolicy(Policy.NEVER);
		return ret;
	}
}
