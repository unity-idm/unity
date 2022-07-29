/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.authn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Date;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.store.api.generic.AuthenticatorConfigurationDB;
import pl.edu.icm.unity.store.api.generic.CredentialDB;
import pl.edu.icm.unity.store.api.generic.NamedCRUDDAOWithTS;
import pl.edu.icm.unity.store.objstore.AbstractNamedWithTSTest;
import pl.edu.icm.unity.store.types.AuthenticatorConfiguration;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.CredentialDefinition;

public class AuthenticatorInstanceTest extends AbstractNamedWithTSTest<AuthenticatorConfiguration>
{
	@Autowired
	private AuthenticatorConfigurationDB dao;
	
	@Autowired
	private CredentialDB credentialDB;
	
	@Override
	protected NamedCRUDDAOWithTS<AuthenticatorConfiguration> getDAO()
	{
		return dao;
	}

	@Test
	public void credentialRemovalIsBlockedByAuthenticator()
	{
		tx.runInTransaction(() -> {
			CredentialDefinition cred = new CredentialDefinition("typeId", "localCred", 
					new I18nString("dName"), new I18nString("desc"));
			cred.setConfiguration("");
			credentialDB.create(cred);
			
			AuthenticatorConfiguration obj = getObject("name1");
			dao.create(obj);

			Throwable error = catchThrowable(() -> credentialDB.delete("localCred"));
			assertThat(error).isInstanceOf(IllegalArgumentException.class);
		});
	}

	@Test
	public void credentialUpdateRefreshesAuthenticatorTS()
	{
		tx.runInTransaction(() -> {
			CredentialDefinition cred = new CredentialDefinition("typeId", "localCred", 
					new I18nString("dName"), new I18nString("desc"));
			cred.setConfiguration("");
			credentialDB.create(cred);
			
			AuthenticatorConfiguration obj = getObject("name1");
			dao.create(obj);

			Date ts = dao.getUpdateTimestamp("name1");
			try
			{
				Thread.sleep(2);
			} catch (Exception e)
			{
			}
			
			credentialDB.updateByName("localCred", cred);
			Date ts2 = dao.getUpdateTimestamp("name1");
			
			assertThat(ts2.getTime() > ts.getTime(), is(true));
		});
	}	
	@Override
	protected AuthenticatorConfiguration getObject(String id)
	{
		return new AuthenticatorConfiguration(id, "verificationMethod", "", "localCred", 1);
	}

	@Override
	protected AuthenticatorConfiguration mutateObject(AuthenticatorConfiguration ret)
	{
		return new AuthenticatorConfiguration(ret.getName(), "verificationMethod2", "sss", "localCred2", 2);
	}
}
