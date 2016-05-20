/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.reg.form;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.store.api.generic.RegistrationFormDB;
import pl.edu.icm.unity.store.impl.objstore.ObjectStoreDAO;
import pl.edu.icm.unity.store.objstore.GenericObjectsDAOImpl;
import pl.edu.icm.unity.store.objstore.credreq.CredentialRequirementDBImpl;
import pl.edu.icm.unity.types.registration.RegistrationForm;

/**
 * Easy access to {@link RegistrationForm} storage.
 * @author K. Benedyczak
 */
@Component
public class RegistrationFormDBImpl extends GenericObjectsDAOImpl<RegistrationForm> implements RegistrationFormDB
{
	@Autowired
	public RegistrationFormDBImpl(RegistrationFormHandler handler, ObjectStoreDAO dbGeneric,
			CredentialRequirementDBImpl credReqDAO)
	{
		super(handler, dbGeneric, RegistrationForm.class, "registration form");
		credReqDAO.addRemovalHandler(this::restrictCredReqRemoval);
		
	}
	
	private void restrictCredReqRemoval(long removedId, String removedName)
	{
		List<RegistrationForm> forms = getAll();
		for (RegistrationForm form: forms)
		{
			if (form.getDefaultCredentialRequirement().equals(removedName))
				throw new IllegalArgumentException("The credential requirement "
						+ "is used by a registration form " + form.getName());
		}
	}	

}
