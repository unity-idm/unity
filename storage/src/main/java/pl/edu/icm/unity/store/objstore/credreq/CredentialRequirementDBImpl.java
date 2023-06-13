/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.credreq;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.authn.CredentialRequirements;
import pl.edu.icm.unity.store.api.generic.CredentialRequirementDB;
import pl.edu.icm.unity.store.impl.objstore.ObjectStoreDAO;
import pl.edu.icm.unity.store.objstore.GenericObjectsDAOImpl;
import pl.edu.icm.unity.store.objstore.cred.CredentialDBImpl;

/**
 * Easy access to {@link CredentialRequirements} storage.
 * <p>
 * Adds consistency checking: credential can not be removed if used in one of credential requirements.
 * @author K. Benedyczak
 */
@Component
public class CredentialRequirementDBImpl extends GenericObjectsDAOImpl<CredentialRequirements>
			implements CredentialRequirementDB
{
	@Autowired
	public CredentialRequirementDBImpl(CredentialRequirementHandler handler,
			ObjectStoreDAO dbGeneric, CredentialDBImpl credentialDB)
	{
		super(handler, dbGeneric, CredentialRequirements.class,	"credential requirement");
		credentialDB.addRemovalHandler(this::restrictCredentialRemoval);
	}
	
	
	private void restrictCredentialRemoval(long removedId, String removedName)
	{
		List<CredentialRequirements> crs = getAll();
		for (CredentialRequirements cr: crs)
		{
			if (cr.getRequiredCredentials().contains(removedName))
				throw new IllegalArgumentException("The credential is used by a credential requirement " 
						+ cr.getName());
		}
	}	
}
