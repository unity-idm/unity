/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.objhandlers;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.AttributeClassUtil;
import pl.edu.icm.unity.server.utils.DefaultGenericObjectHandler;

@Component
public class AttributeClassObjectHandler extends DefaultGenericObjectHandler
{
	public AttributeClassObjectHandler()
	{
		super(AttributeClassUtil.ATTRIBUTE_CLASS_OBJECT_TYPE);
	}
}
