/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.ac;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.store.api.generic.GenericObjectsDAO;
import pl.edu.icm.unity.store.objstore.GenericObjectIEBase;
import pl.edu.icm.unity.types.basic.AttributesClass;

/**
 * Handles import/export of entities.
 * @author K. Benedyczak
 */
@Component
public class AttributeClassIE extends GenericObjectIEBase<AttributesClass>
{
	@Autowired
	public AttributeClassIE(GenericObjectsDAO<AttributesClass> dao, ObjectMapper jsonMapper)
	{
		super(dao, jsonMapper, AttributesClass.class);
	}
}



