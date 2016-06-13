/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.credreq;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.store.api.generic.CredentialDB;
import pl.edu.icm.unity.store.api.generic.CredentialRequirementDB;
import pl.edu.icm.unity.store.api.generic.NamedCRUDDAOWithTS;
import pl.edu.icm.unity.store.objstore.AbstractNamedWithTSTest;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialRequirements;

public class CredReqTest extends AbstractNamedWithTSTest<CredentialRequirements>
{
	@Autowired
	private CredentialRequirementDB dao;
	
	@Autowired
	private CredentialDB credentialDB;
	
	@Override
	protected NamedCRUDDAOWithTS<CredentialRequirements> getDAO()
	{
		return dao;
	}

	@Test
	public void credentialRemovalIsBlockedByCR()
	{
		tx.runInTransaction(() -> {
			CredentialDefinition cred = new CredentialDefinition("typeId", "cred1", 
					new I18nString("dName"), new I18nString("desc"));
			cred.setJsonConfiguration(Constants.MAPPER.createObjectNode());
			credentialDB.create(cred);
			
			CredentialRequirements obj = getObject("name1");
			dao.create(obj);

			catchException(credentialDB).delete("cred1");
			assertThat(caughtException(), isA(IllegalArgumentException.class));
		});
	}

	@Override
	protected CredentialRequirements getObject(String id)
	{
		CredentialRequirements ret = new CredentialRequirements();
		ret.setDescription("description");
		ret.setName(id);
		ret.setRequiredCredentials(Sets.newHashSet("cred1", "cred2"));
		return ret;
	}

	@Override
	protected CredentialRequirements mutateObject(CredentialRequirements ret)
	{
		ret.setDescription("description2");
		ret.setName("name-changed");
		ret.setRequiredCredentials(Sets.newHashSet("cred3"));
		return ret;
	}
}
