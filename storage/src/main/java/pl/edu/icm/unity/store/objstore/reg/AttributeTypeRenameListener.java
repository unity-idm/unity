/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.reg;

import java.util.List;

import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.base.registration.BaseForm;
import pl.edu.icm.unity.store.ReferenceUpdateHandler;
import pl.edu.icm.unity.store.api.generic.NamedCRUDDAOWithTS;

public class AttributeTypeRenameListener<T extends BaseForm > implements ReferenceUpdateHandler<AttributeType>
{
	private NamedCRUDDAOWithTS<T> dao;
	
	public AttributeTypeRenameListener(NamedCRUDDAOWithTS<T> dao)
	{
		this.dao = dao;
	}

	@Override
	public void preUpdateCheck(PlannedUpdateEvent<AttributeType> update)
	{
		if (update.modifiedName.equals(update.newValue.getName()))
			return;
		List<T> forms = dao.getAll();
		for (T form: forms)
		{
			for (AttributeRegistrationParam aParam: form.getAttributeParams())
				if (update.modifiedName.equals(aParam.getAttributeType()))
				{
					aParam.setAttributeType(update.newValue.getName());
					dao.update(form);
				}
		}
	}
}