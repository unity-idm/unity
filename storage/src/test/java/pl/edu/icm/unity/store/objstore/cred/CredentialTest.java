/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.cred;

import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.store.api.generic.CredentialDB;
import pl.edu.icm.unity.store.api.generic.NamedCRUDDAOWithTS;
import pl.edu.icm.unity.store.objstore.AbstractNamedWithTSTest;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.CredentialDefinition;

public class CredentialTest extends AbstractNamedWithTSTest<CredentialDefinition>
{
	@Autowired
	private CredentialDB credentialDB;
	
	@Override
	protected NamedCRUDDAOWithTS<CredentialDefinition> getDAO()
	{
		return credentialDB;
	}

	@Override
	protected CredentialDefinition getObject(String id)
	{
		CredentialDefinition ret = new CredentialDefinition("typeId", id, 
				new I18nString("dName"), new I18nString("desc"));
		ret.setJsonConfiguration(Constants.MAPPER.createObjectNode());
		return ret;
	}

	@Override
	protected CredentialDefinition mutateObject(CredentialDefinition src)
	{
		src.setName("name-Changed");
		src.setDescription(new I18nString("dName2"));
		src.setDisplayedName(new I18nString("dName2"));
		ObjectNode cfg = Constants.MAPPER.createObjectNode();
		cfg.put("test", "val");
		src.setJsonConfiguration(cfg);
		return src;
	}
}
