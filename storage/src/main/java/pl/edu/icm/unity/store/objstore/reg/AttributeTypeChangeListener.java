/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.reg;

import java.util.List;

import pl.edu.icm.unity.base.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.base.registration.BaseForm;
import pl.edu.icm.unity.store.ReferenceRemovalHandler;
import pl.edu.icm.unity.store.api.generic.NamedCRUDDAOWithTS;

public class AttributeTypeChangeListener implements ReferenceRemovalHandler
{
	private NamedCRUDDAOWithTS<? extends BaseForm> dao;
	
	public AttributeTypeChangeListener(NamedCRUDDAOWithTS<? extends BaseForm> dao)
	{
		this.dao = dao;
	}

	@Override
	public void preRemoveCheck(long removedId, String removedName)
	{
		List<? extends BaseForm> forms = dao.getAll();
		for (BaseForm form: forms)
		{
			for (AttributeRegistrationParam attr: form.getAttributeParams())
				if (attr.getAttributeType().equals(removedName))
					throw new IllegalArgumentException("The attribute type is used "
							+ "by an attribute in a form " + form.getName());
		}
	}
}