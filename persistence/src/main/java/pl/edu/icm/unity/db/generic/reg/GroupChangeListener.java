/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.generic.reg;

import java.util.List;

import org.apache.ibatis.session.SqlSession;

import pl.edu.icm.unity.db.DBGroups;
import pl.edu.icm.unity.db.generic.DependencyChangeListener;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.SchemaConsistencyException;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;

public class GroupChangeListener implements DependencyChangeListener<Group>
{
	private FormsSupplier supplier;
	
	public GroupChangeListener(FormsSupplier supplier)
	{
		this.supplier = supplier;
	}

	@Override
	public String getDependencyObjectType()
	{
		return DBGroups.GROUPS_NOTIFICATION_ID;
	}

	@Override
	public void preAdd(Group newObject, SqlSession sql) throws EngineException { }
	@Override
	public void preUpdate(Group oldObject,
			Group updatedObject, SqlSession sql) throws EngineException {}

	@Override
	public void preRemove(Group removedObject, SqlSession sql)
			throws EngineException
	{
		List<? extends BaseForm> forms = supplier.getForms(sql);
		for (BaseForm form: forms)
		{
			for (GroupRegistrationParam group: form.getGroupParams())
				if (group.getGroupPath().startsWith(removedObject.toString()))
					throw new SchemaConsistencyException("The group is used by a form " 
						+ form.getName());
			for (AttributeRegistrationParam attr: form.getAttributeParams())
				if (attr.getGroup().startsWith(removedObject.toString()))
					throw new SchemaConsistencyException("The group is used by an attribute in a form " 
						+ form.getName());
			if (form.getNotificationsConfiguration() != null && 
					removedObject.toString().equals(form.getNotificationsConfiguration().
							getAdminsNotificationGroup()))
				throw new SchemaConsistencyException("The group is used as administrators notification group in a form " 
						+ form.getName());
		}
	}
}