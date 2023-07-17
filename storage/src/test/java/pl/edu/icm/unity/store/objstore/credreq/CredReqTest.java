/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.credreq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

import pl.edu.icm.unity.base.authn.CredentialDefinition;
import pl.edu.icm.unity.base.authn.CredentialRequirements;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.store.api.generic.CredentialDB;
import pl.edu.icm.unity.store.api.generic.CredentialRequirementDB;
import pl.edu.icm.unity.store.api.generic.NamedCRUDDAOWithTS;
import pl.edu.icm.unity.store.objstore.AbstractNamedWithTSTest;

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
			cred.setConfiguration("");
			credentialDB.create(cred);
			
			CredentialRequirements obj = getObject("name1");
			dao.create(obj);

			Throwable error = catchThrowable(() -> credentialDB.delete("cred1"));
			assertThat(error).isInstanceOf(IllegalArgumentException.class);
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
