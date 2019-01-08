/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.ac;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.store.api.generic.AttributeClassDB;
import pl.edu.icm.unity.store.impl.objstore.ObjectStoreDAO;
import pl.edu.icm.unity.store.objstore.GenericObjectsDAOImpl;
import pl.edu.icm.unity.types.basic.AttributesClass;

/**
 * Easy access to {@link AttributesClass} storage.
 * @author K. Benedyczak
 */
@Component
public class AttributeClassDBImpl extends GenericObjectsDAOImpl<AttributesClass> implements AttributeClassDB 
{
	@Autowired
	AttributeClassDBImpl(AttributeClassHandler handler,
			ObjectStoreDAO dbGeneric)
	{
		super(handler, dbGeneric, AttributesClass.class, "attributes class");
	}
}
