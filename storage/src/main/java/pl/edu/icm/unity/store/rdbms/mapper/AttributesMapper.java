/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms.mapper;

import java.util.List;

import pl.edu.icm.unity.store.rdbms.model.AttributeBean;



/**
 * Access to the Attributes.xml operations.
 * @author K. Benedyczak
 */
public interface AttributesMapper
{
	public List<AttributeBean> getAttributes(AttributeBean a);
	public void insertAttribute(AttributeBean a);
	public void updateAttribute(AttributeBean a);
	public void deleteAttribute(AttributeBean a);
	public void deleteAttributesInGroup(AttributeBean a);
}
