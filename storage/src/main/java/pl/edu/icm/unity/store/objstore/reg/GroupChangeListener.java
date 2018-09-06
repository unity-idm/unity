/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.reg;

import java.util.List;

import pl.edu.icm.unity.store.ReferenceRemovalHandler;
import pl.edu.icm.unity.store.api.generic.NamedCRUDDAOWithTS;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;

public class GroupChangeListener implements ReferenceRemovalHandler
{
	private NamedCRUDDAOWithTS<? extends BaseForm> dao;
	
	public GroupChangeListener(NamedCRUDDAOWithTS<? extends BaseForm> dao)
	{
		this.dao = dao;
	}

	@Override
	public void preRemoveCheck(long removedId, String removedName)
	{
		List<? extends BaseForm> forms = dao.getAll();
		for (BaseForm form: forms)
		{
			for (GroupRegistrationParam group: form.getGroupParams())
				if (group.getGroupPath().startsWith(removedName))
					throw new IllegalArgumentException("The group is used by a form " 
						+ form.getName());
			for (AttributeRegistrationParam attr: form.getAttributeParams())
				if (!attr.isUsingDynamicGroup() && attr.getGroup().startsWith(removedName))
					throw new IllegalArgumentException("The group is used by an attribute in a form " 
						+ form.getName());
			if (form.getNotificationsConfiguration() != null && 
					removedName.equals(form.getNotificationsConfiguration().
							getAdminsNotificationGroup()))
				throw new IllegalArgumentException("The group is used as administrators notification group in a form " 
						+ form.getName());
		}		
	}
}