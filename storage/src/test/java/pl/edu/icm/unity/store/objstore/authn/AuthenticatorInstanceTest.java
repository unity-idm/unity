/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.authn;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.assertThat;

import java.util.Date;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.store.api.generic.AuthenticatorInstanceDB;
import pl.edu.icm.unity.store.api.generic.CredentialDB;
import pl.edu.icm.unity.store.api.generic.NamedCRUDDAOWithTS;
import pl.edu.icm.unity.store.objstore.AbstractNamedWithTSTest;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticatorInstance;
import pl.edu.icm.unity.types.authn.AuthenticatorTypeDescription;
import pl.edu.icm.unity.types.authn.CredentialDefinition;

public class AuthenticatorInstanceTest extends AbstractNamedWithTSTest<AuthenticatorInstance>
{
	@Autowired
	private AuthenticatorInstanceDB dao;
	
	@Autowired
	private CredentialDB credentialDB;
	
	@Override
	protected NamedCRUDDAOWithTS<AuthenticatorInstance> getDAO()
	{
		return dao;
	}

	@Test
	public void credentialRemovalIsBlockedByAuthenticator()
	{
		tx.runInTransaction(() -> {
			CredentialDefinition cred = new CredentialDefinition("typeId", "localCred", 
					new I18nString("dName"), new I18nString("desc"));
			cred.setJsonConfiguration(Constants.MAPPER.createObjectNode());
			credentialDB.create(cred);
			
			AuthenticatorInstance obj = getObject("name1");
			dao.create(obj);

			catchException(credentialDB).delete("localCred");
			assertThat(caughtException(), isA(IllegalArgumentException.class));
		});
	}

	@Test
	public void credentialUpdateRefreshesAuthenticatorTS()
	{
		tx.runInTransaction(() -> {
			CredentialDefinition cred = new CredentialDefinition("typeId", "localCred", 
					new I18nString("dName"), new I18nString("desc"));
			cred.setJsonConfiguration(Constants.MAPPER.createObjectNode());
			credentialDB.create(cred);
			
			AuthenticatorInstance obj = getObject("name1");
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
	protected AuthenticatorInstance getObject(String id)
	{
		AuthenticatorInstance ret = new AuthenticatorInstance();
		ret.setId(id);
		ret.setLocalCredentialName("localCred");
		ret.setRetrievalJsonConfiguration("rc");
		AuthenticatorTypeDescription typeDesc = new AuthenticatorTypeDescription();
		typeDesc.setId(id);
		typeDesc.setLocal(true);
		typeDesc.setRetrievalMethod("retrievalMethod");
		typeDesc.setRetrievalMethodDescription("retrievalMethodDescription");
		typeDesc.setSupportedBinding("supportedBinding");
		typeDesc.setVerificationMethod("verificationMethod");
		typeDesc.setVerificationMethodDescription("vmd");
		ret.setTypeDescription(typeDesc);
		ret.setVerificatorJsonConfiguration("vc");
		return ret;
	}

	@Override
	protected AuthenticatorInstance mutateObject(AuthenticatorInstance ret)
	{
		ret.setLocalCredentialName("localCred2");
		ret.setRetrievalJsonConfiguration("rc2");
		AuthenticatorTypeDescription typeDesc = new AuthenticatorTypeDescription();
		typeDesc.setLocal(true);
		typeDesc.setRetrievalMethod("retrievalMethod2");
		typeDesc.setRetrievalMethodDescription("retrievalMethodDescription2");
		typeDesc.setSupportedBinding("supportedBinding2");
		typeDesc.setVerificationMethod("verificationMethod2");
		typeDesc.setVerificationMethodDescription("vmd2");
		ret.setTypeDescription(typeDesc);
		ret.setVerificatorJsonConfiguration("vc2");
		return ret;
	}
}
