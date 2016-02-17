/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.generic.reg;

import java.util.List;

import org.apache.ibatis.session.SqlSession;

import pl.edu.icm.unity.db.DBAttributes;
import pl.edu.icm.unity.db.generic.DependencyChangeListener;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.SchemaConsistencyException;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.BaseForm;

public class AttributeTypeChangeListener implements DependencyChangeListener<AttributeType>
{
	private FormsSupplier supplier;
	
	public AttributeTypeChangeListener(FormsSupplier supplier)
	{
		this.supplier = supplier;
	}

	@Override
	public String getDependencyObjectType()
	{
		return DBAttributes.ATTRIBUTE_TYPES_NOTIFICATION_ID;
	}

	@Override
	public void preAdd(AttributeType newObject, SqlSession sql) throws EngineException { }
	
	@Override
	public void preUpdate(AttributeType oldObject,
			AttributeType updatedObject, SqlSession sql) throws EngineException {}

	@Override
	public void preRemove(AttributeType removedObject, SqlSession sql)
			throws EngineException
	{
		List<? extends BaseForm> forms = supplier.getForms(sql);
		for (BaseForm form: forms)
		{
			for (AttributeRegistrationParam attr: form.getAttributeParams())
				if (attr.getAttributeType().equals(removedObject.getName()))
					throw new SchemaConsistencyException("The attribute type is used "
							+ "by an attribute in a form " + form.getName());
		}
	}
}