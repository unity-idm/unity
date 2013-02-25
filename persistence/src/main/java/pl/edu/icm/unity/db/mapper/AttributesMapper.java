/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.mapper;

import java.util.List;

import pl.edu.icm.unity.db.model.AttributeBean;
import pl.edu.icm.unity.db.model.AttributeTypeBean;



/**
 * Access to the Attributes.xml operations.
 * @author K. Benedyczak
 */
public interface AttributesMapper
{
	public void insertAttributeType(AttributeTypeBean at);
	public List<AttributeTypeBean> getAttributeTypes();
	public AttributeTypeBean getAttributeType(String name);
	
	public AttributeBean getSpecificAttribute(AttributeBean a);
	public List<AttributeBean> getInGroupAttributes(AttributeBean a);
	public void insertAttribute(AttributeBean a);
	public void updateAttribute(AttributeBean a);
	public void deleteAttribute(AttributeBean a);
}
