/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.reg;

import java.util.List;

import pl.edu.icm.unity.store.ReferenceUpdateHandler;
import pl.edu.icm.unity.store.api.generic.NamedCRUDDAOWithTS;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.BaseForm;

public class AttributeTypeRenameListener<T extends BaseForm > implements ReferenceUpdateHandler<AttributeType>
{
	private NamedCRUDDAOWithTS<T> dao;
	
	public AttributeTypeRenameListener(NamedCRUDDAOWithTS<T> dao)
	{
		this.dao = dao;
	}

	@Override
	public void preUpdateCheck(long modifiedId, String modifiedName,
			AttributeType newValue)
	{
		if (modifiedName.equals(newValue.getName()))
			return;
		List<T> forms = dao.getAll();
		for (T form: forms)
		{
			for (AttributeRegistrationParam aParam: form.getAttributeParams())
				if (modifiedName.equals(aParam.getAttributeType()))
				{
					aParam.setAttributeType(newValue.getName());
					dao.update(form);
				}
		}
	}
}