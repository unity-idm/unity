/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.reg;

import java.util.List;

import pl.edu.icm.unity.base.registration.BaseForm;
import pl.edu.icm.unity.base.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.store.ReferenceRemovalHandler;
import pl.edu.icm.unity.store.api.generic.NamedCRUDDAOWithTS;

public class CredentialChangeListener implements ReferenceRemovalHandler
{
	private NamedCRUDDAOWithTS<? extends BaseForm> dao;
	
	public CredentialChangeListener(NamedCRUDDAOWithTS<? extends BaseForm> dao)
	{
		this.dao = dao;
	}

	@Override
	public void preRemoveCheck(long removedId, String removedName)
	{
		List<? extends BaseForm> forms = dao.getAll();
		for (BaseForm form: forms)
		{
			for (CredentialRegistrationParam crParam: form.getCredentialParams())
				if (removedName.equals(crParam.getCredentialName()))
					throw new IllegalArgumentException("The credential is used by a form " 
						+ form.getName());
		}
	}
}