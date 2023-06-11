/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.authnFlow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import pl.edu.icm.unity.base.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.base.authn.AuthenticationFlowDefinition.Policy;
import pl.edu.icm.unity.store.api.generic.AuthenticationFlowDB;
import pl.edu.icm.unity.store.api.generic.AuthenticatorConfigurationDB;
import pl.edu.icm.unity.store.api.generic.NamedCRUDDAOWithTS;
import pl.edu.icm.unity.store.objstore.AbstractNamedWithTSTest;
import pl.edu.icm.unity.store.types.AuthenticatorConfiguration;

public class AuthenticationFlowTest extends AbstractNamedWithTSTest<AuthenticationFlowDefinition>
{
	@Autowired
	private AuthenticationFlowDB dao;
	
	@Autowired
	private AuthenticatorConfigurationDB authDB;
	
	@Override
	protected NamedCRUDDAOWithTS<AuthenticationFlowDefinition> getDAO()
	{
		return dao;
	}

	@Test
	public void authenticatorRemovalIsBlockedByAuthenticationFlow()
	{
		tx.runInTransaction(() -> {
			
			AuthenticatorConfiguration ret = new AuthenticatorConfiguration("pass", "", "", "", 1);
			authDB.create(ret);
			
			AuthenticationFlowDefinition obj = getObject("name1");
			dao.create(obj);

			Throwable error = catchThrowable(() -> authDB.delete("pass"));
			assertThat(error).isInstanceOf(IllegalArgumentException.class);
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
