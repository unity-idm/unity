/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attributetype;

import pl.edu.icm.unity.store.ReferenceAwareDAO;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.types.basic.AttributeType;

/**
 * Extends {@link AttributeTypeDAO} with {@link ReferenceAwareDAO} methods, which shall be visible only internally
 * in this module.
 * @author K. Benedyczak
 */
public interface AttributeTypeDAOInternal extends AttributeTypeDAO, ReferenceAwareDAO<AttributeType>
{
}
