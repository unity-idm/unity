/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.reg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.authn.CredentialDefinition;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.registration.UserRequestState;
import pl.edu.icm.unity.store.api.generic.CredentialDB;
import pl.edu.icm.unity.store.objstore.AbstractNamedWithTSTest;

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
			cred.setConfiguration("");
			credentialDB.create(cred);
			
			T obj = getObject("name1");
			getDAO().create(obj);

			Throwable error = catchThrowable(() -> credentialDB.update(cred));
			assertThat(error).isInstanceOf(IllegalArgumentException.class);
		});
	}
}
