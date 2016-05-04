/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attribute;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Date;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.store.AbstractBasicDAOTest;
import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.IdentityTypeDAO;
import pl.edu.icm.unity.store.api.StoredAttribute;
import pl.edu.icm.unity.store.api.StoredEntity;
import pl.edu.icm.unity.store.mocks.MockAttributeSyntax;
import pl.edu.icm.unity.store.mocks.MockIdentityTypeDef;
import pl.edu.icm.unity.types.EntityInformation;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.IdentityType;

public class AttributeTest extends AbstractBasicDAOTest<StoredAttribute>
{
	@Autowired
	private AttributeDAO dao;
	@Autowired
	private GroupDAO groupDAO;
	@Autowired
	private EntityDAO entityDAO;
	@Autowired
	private IdentityTypeDAO itDao;
	@Autowired
	private AttributeTypeDAO atDao;

	
	private long entityId;
	
	@Before
	public void init()
	{
		tx.runInTransaction(() -> {
			itDao.create(new IdentityType(new MockIdentityTypeDef()));
			entityId = entityDAO.create(new StoredEntity(null, 
					new EntityInformation(EntityState.valid)));
			groupDAO.create(new Group("/A"));
			atDao.create(new AttributeType("attr", new MockAttributeSyntax()));
		});
	}
	
	@Override
	protected AttributeDAO getDAO()
	{
		return dao;
	}

	@Override
	protected StoredAttribute getObject(String id)
	{
		AttributeExt<String> a = new AttributeExt<>();
		a.setAttributeSyntax(new MockAttributeSyntax());
		a.setName("attr");
		a.setGroupPath("/A");
		a.setRemoteIdp("remoteIdp");
		a.setTranslationProfile("translationProfile");
		a.setVisibility(AttributeVisibility.full);
		a.setValues(Lists.newArrayList("v1", "v2"));
		a.setDirect(true);
		a.setCreationTs(new Date(100));
		a.setUpdateTs(new Date(1000));
		return new StoredAttribute(a, entityId);
	}

	@Override
	protected void mutateObject(StoredAttribute src)
	{
		@SuppressWarnings("unchecked")
		AttributeExt<String> a = (AttributeExt<String>) src.getAttribute();
		a.setRemoteIdp("remoteIdp2");
		a.setTranslationProfile("translationProfile2");
		a.setVisibility(AttributeVisibility.local);
		a.setValues(Lists.newArrayList("w1"));
		a.setUpdateTs(new Date(2000));
	}

	@Override
	protected void assertAreEqual(StoredAttribute obj, StoredAttribute cmp)
	{
		assertThat(obj, is(cmp));
	}
}
