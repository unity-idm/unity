/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.reg;

import java.util.List;

import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.base.registration.BaseForm;
import pl.edu.icm.unity.base.registration.BaseFormNotifications;
import pl.edu.icm.unity.base.registration.GroupRegistrationParam;
import pl.edu.icm.unity.store.ReferenceUpdateHandler;
import pl.edu.icm.unity.store.api.generic.NamedCRUDDAOWithTS;

public class GroupRenameListener<T extends BaseForm> implements ReferenceUpdateHandler<Group>
{
	private NamedCRUDDAOWithTS<T> dao;
	
	public GroupRenameListener(NamedCRUDDAOWithTS<T> dao)
	{
		this.dao = dao;
	}

	@Override
	public void preUpdateCheck(PlannedUpdateEvent<Group> update)
	{
		String newGroup = update.newValue.getName();
		String modifiedName = update.modifiedName;
		if (modifiedName.equals(newGroup))
			return;
		List<T> forms = dao.getAll();
		for (T form: forms)
		{
			boolean updateNeeded = false;
			for (GroupRegistrationParam group: form.getGroupParams())
				if (Group.isChildOrSame(group.getGroupPath(), modifiedName))
				{
					group.setGroupPath(Group.renameParent(group.getGroupPath(), 
							modifiedName, newGroup));
					updateNeeded = true;
				}
			for (AttributeRegistrationParam attr: form.getAttributeParams())
				if (!attr.isUsingDynamicGroup() && Group.isChildOrSame(attr.getGroup(), modifiedName))
				{
					attr.setGroup(Group.renameParent(attr.getGroup(), 
							modifiedName, newGroup));
					updateNeeded = true;
				}
			BaseFormNotifications baseNotCfg = form.getNotificationsConfiguration();
			if (baseNotCfg != null && Group.isChildOrSame(baseNotCfg.getAdminsNotificationGroup(), modifiedName))
			{
				baseNotCfg.setAdminsNotificationGroup(Group.renameParent(
						baseNotCfg.getAdminsNotificationGroup(), modifiedName, newGroup));
				updateNeeded = true;
			}

			if (updateNeeded)
				dao.update(form);
		}		
	}
}