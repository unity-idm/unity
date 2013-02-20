/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.mapper;

import java.util.List;

import pl.edu.icm.unity.db.model.BaseBean;



/**
 * Access to the Attributes.xml operations.
 * @author K. Benedyczak
 */
public interface AttributesMapper
{
	public void insertAttributeType(BaseBean at);
	public List<BaseBean> getAttributeTypes();
	public BaseBean getAttributeType(String name);
}
