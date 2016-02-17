/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.generic.reg;

import java.util.List;

import org.apache.ibatis.session.SqlSession;

import pl.edu.icm.unity.db.generic.DependencyChangeListener;
import pl.edu.icm.unity.db.generic.cred.CredentialHandler;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.SchemaConsistencyException;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.CredentialRegistrationParam;

public class CredentialChangeListener implements DependencyChangeListener<CredentialDefinition>
{
	private FormsSupplier supplier;

	public CredentialChangeListener(FormsSupplier supplier)
	{
		this.supplier = supplier;
	}

	@Override
	public String getDependencyObjectType()
	{
		return CredentialHandler.CREDENTIAL_OBJECT_TYPE;
	}

	@Override
	public void preAdd(CredentialDefinition newObject, SqlSession sql) throws EngineException { }
	@Override
	public void preUpdate(CredentialDefinition oldObject,
			CredentialDefinition updatedObject, SqlSession sql) throws EngineException {}

	@Override
	public void preRemove(CredentialDefinition removedObject, SqlSession sql)
			throws EngineException
	{
		List<? extends BaseForm> forms = supplier.getForms(sql);
		for (BaseForm form: forms)
		{
			for (CredentialRegistrationParam crParam: form.getCredentialParams())
				if (removedObject.getName().equals(crParam.getCredentialName()))
					throw new SchemaConsistencyException("The credential is used by a form " 
						+ form.getName());
		}
	}
}