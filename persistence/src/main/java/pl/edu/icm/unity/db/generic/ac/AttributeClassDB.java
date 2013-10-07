/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.generic.ac;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBGeneric;
import pl.edu.icm.unity.db.generic.DependencyNotificationManager;
import pl.edu.icm.unity.db.generic.GenericObjectsDB;
import pl.edu.icm.unity.types.basic.AttributesClass;

/**
 * Easy access to {@link AttributesClass} storage.
 * @author K. Benedyczak
 */
@Component
public class AttributeClassDB extends GenericObjectsDB<AttributesClass>
{
	@Autowired
	public AttributeClassDB(AttributeClassHandler handler,
			DBGeneric dbGeneric, DependencyNotificationManager notificationManager)
	{
		super(handler, dbGeneric, notificationManager, AttributesClass.class,
				"attributes class");
	}
}
