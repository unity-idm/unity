/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.reg;

import java.util.List;

import pl.edu.icm.unity.store.ReferenceUpdateHandler;
import pl.edu.icm.unity.store.api.generic.NamedCRUDDAOWithTS;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.CredentialRegistrationParam;

public class CredentialRenameListener<T extends BaseForm > implements ReferenceUpdateHandler<CredentialDefinition>
{
	private NamedCRUDDAOWithTS<T> dao;
	
	public CredentialRenameListener(NamedCRUDDAOWithTS<T> dao)
	{
		this.dao = dao;
	}

	@Override
	public void preUpdateCheck(PlannedUpdateEvent<CredentialDefinition> update)
	{
		if (update.modifiedName.equals(update.newValue.getName()))
			return;
		List<T> forms = dao.getAll();
		for (T form: forms)
		{
			for (CredentialRegistrationParam crParam: form.getCredentialParams())
				if (update.modifiedName.equals(crParam.getCredentialName()))
				{
					crParam.setCredentialName(update.newValue.getName());
					dao.update(form);
				}
		}
	}
}