/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api;

import pl.edu.icm.unity.base.attribute.AttributeType;

/**
 * {@link AttributeType} DAO
 * @author K. Benedyczak
 */
public interface AttributeTypeDAO extends NamedCRUDDAO<AttributeType>
{
	String DAO_ID = "attributeTypesDAO";
}
