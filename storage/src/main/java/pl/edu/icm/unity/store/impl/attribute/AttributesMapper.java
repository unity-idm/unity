/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attribute;

import java.util.List;

import pl.edu.icm.unity.store.rdbms.BasicCRUDMapper;



/**
 * Access to the Attributes.xml operations.
 * @author K. Benedyczak
 */
public interface AttributesMapper extends BasicCRUDMapper<AttributeBean>
{
	void updateAttribute(AttributeBean a);
	void deleteAttribute(AttributeBean a);
	void deleteAttributesInGroup(AttributeBean a);

	List<AttributeBean> getAttributes(AttributeBean a);
}
