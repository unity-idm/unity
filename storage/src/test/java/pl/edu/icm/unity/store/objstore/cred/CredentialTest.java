/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.cred;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.store.api.generic.CredentialDB;
import pl.edu.icm.unity.store.api.generic.GenericObjectsDAO;
import pl.edu.icm.unity.store.objstore.AbstractObjStoreTest;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.CredentialDefinition;

public class CredentialTest extends AbstractObjStoreTest<CredentialDefinition>
{
	@Autowired
	private CredentialDB credentialDB;
	
	@Override
	protected GenericObjectsDAO<CredentialDefinition> getDAO()
	{
		return credentialDB;
	}

	@Override
	protected CredentialDefinition getObject(String id)
	{
		CredentialDefinition ret = new CredentialDefinition("typeId", id, 
				new I18nString("dName"), new I18nString("desc"));
		ret.setJsonConfiguration("{}");
		return ret;
	}

	@Override
	protected CredentialDefinition mutateObject(CredentialDefinition src)
	{
		src.setName("name-Changed");
		src.setDescription(new I18nString("dName2"));
		src.setDisplayedName(new I18nString("dName2"));
		src.setJsonConfiguration("[1,2,3]");
		return src;
	}

	@Override
	protected void assertAreEqual(CredentialDefinition obj, CredentialDefinition cmp)
	{
		assertThat(obj, is(cmp));
	}
}
