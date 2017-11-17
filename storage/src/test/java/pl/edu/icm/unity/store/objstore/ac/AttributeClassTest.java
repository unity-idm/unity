/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.ac;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

import pl.edu.icm.unity.store.api.generic.AttributeClassDB;
import pl.edu.icm.unity.store.api.generic.NamedCRUDDAOWithTS;
import pl.edu.icm.unity.store.objstore.AbstractNamedWithTSTest;
import pl.edu.icm.unity.types.basic.AttributesClass;

public class AttributeClassTest extends AbstractNamedWithTSTest<AttributesClass>
{
	@Autowired
	private AttributeClassDB dao;
	
	@Override
	protected NamedCRUDDAOWithTS<AttributesClass> getDAO()
	{
		return dao;
	}

	@Override
	protected AttributesClass getObject(String id)
	{
		AttributesClass ret = new AttributesClass();
		ret.setAllowArbitrary(false);
		ret.setAllowed(Sets.newHashSet("allowed1", "allowed2"));
		ret.setMandatory(Sets.newHashSet("mandatory"));
		ret.setDescription("description");
		ret.setName(id);
		ret.setParentClasses(Sets.newHashSet("parent"));
		return ret;
	}

	@Override
	protected AttributesClass mutateObject(AttributesClass ret)
	{
		ret.setAllowArbitrary(true);
		ret.setAllowed(Sets.newHashSet("allowed3"));
		ret.setMandatory(Sets.newHashSet("mandatory2"));
		ret.setDescription("description3");
		ret.setName("name-changed");
		ret.setParentClasses(Sets.newHashSet("parent2"));
		return ret;
	}
}
