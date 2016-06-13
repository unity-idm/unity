/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.reg;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.store.api.generic.CredentialDB;
import pl.edu.icm.unity.store.objstore.AbstractNamedWithTSTest;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.registration.UserRequestState;

public abstract class BaseRequestTest<T extends UserRequestState<?>> extends AbstractNamedWithTSTest<T>
{
	@Autowired
	private CredentialDB credentialDB;
	
	@Test
	public void usedCredUpdateIsRestrictedForPending()
	{
		tx.runInTransaction(() -> {
			CredentialDefinition cred = new CredentialDefinition("typeId", "cred", 
					new I18nString("dName"), new I18nString("desc"));
			cred.setJsonConfiguration(Constants.MAPPER.createObjectNode());
			credentialDB.create(cred);
			
			T obj = getObject("name1");
			getDAO().create(obj);

			catchException(credentialDB).update(cred);
			assertThat(caughtException(), isA(IllegalArgumentException.class));
		});
	}
}
