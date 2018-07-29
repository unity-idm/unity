/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.ac;

import pl.edu.icm.unity.store.api.generic.AttributeClassDB;
import pl.edu.icm.unity.store.impl.objstore.ObjectStoreDAO;
import pl.edu.icm.unity.store.objstore.GenericObjectsDAOImpl;
import pl.edu.icm.unity.types.basic.AttributesClass;

/**
 * Easy access to {@link AttributesClass} storage, without cache
 * @author K. Benedyczak
 */
class AttributeClassDBNoChaceImpl extends GenericObjectsDAOImpl<AttributesClass> implements AttributeClassDB 
{
	AttributeClassDBNoChaceImpl(AttributeClassHandler handler,
			ObjectStoreDAO dbGeneric)
	{
		super(handler, dbGeneric, AttributesClass.class, "attributes class");
	}
}
