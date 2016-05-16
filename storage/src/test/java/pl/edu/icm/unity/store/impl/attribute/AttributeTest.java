/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attribute;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.IdentityTypeDAO;
import pl.edu.icm.unity.store.api.StoredAttribute;
import pl.edu.icm.unity.store.api.StoredEntity;
import pl.edu.icm.unity.store.impl.AbstractBasicDAOTest;
import pl.edu.icm.unity.store.mocks.MockIdentityTypeDef;
import pl.edu.icm.unity.types.EntityInformation;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.IdentityType;
import pl.edu.icm.unity.types.basic2.Attribute2;
import pl.edu.icm.unity.types.basic2.AttributeExt2;
import pl.edu.icm.unity.types.basic2.Group;

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
	private long entityId2;

	@Before
	public void cleanDB()
	{
		dbCleaner.reset();
		tx.runInTransaction(() -> {
			itDao.create(new IdentityType(new MockIdentityTypeDef()));
			entityId = entityDAO.create(new StoredEntity(null, 
					new EntityInformation(EntityState.valid)));
			entityId2 = entityDAO.create(new StoredEntity(null, 
					new EntityInformation(EntityState.valid)));
			groupDAO.create(new Group("/A"));
			groupDAO.create(new Group("/B"));
			groupDAO.create(new Group("/C"));
			atDao.create(new AttributeType("attr", "syntax"));
			atDao.create(new AttributeType("attr2", "syntax"));
			atDao.create(new AttributeType("attr3", "syntax"));
		});
	}
	
	
	@Test
	public void allEntityAttributesInGroupAreReturned()
	{
		tx.runInTransaction(() -> {
			AttributeDAO dao = getDAO();
			StoredAttribute obj = getObject("");
			dao.create(obj);
			
			StoredAttribute obj2 = getObject("");
			obj2.getAttribute().setName("attr2");
			dao.create(obj2);
			
			StoredAttribute obj3 = getObject("");
			obj3.getAttribute().setGroupPath("/C");
			dao.create(obj3);
			
			StoredAttribute obj4 = getObject("");
			obj4 = new StoredAttribute(obj4.getAttribute(), entityId2);
			dao.create(obj4);
			
			List<AttributeExt2> attributes = dao.getAttributes(null, 
					obj.getEntityId(), 
					"/A");
			
			assertAllAndOnlyAllIn(Lists.newArrayList(obj.getAttribute(), obj2.getAttribute()), 
					attributes);
		});
	}
	
	@Test
	public void allEntityAttributesAreReturned()
	{
		tx.runInTransaction(() -> {
			AttributeDAO dao = getDAO();
			StoredAttribute obj = getObject("");
			dao.create(obj);
			
			StoredAttribute obj2 = getObject("");
			obj2.getAttribute().setName("attr2");
			dao.create(obj2);
			
			StoredAttribute obj3 = getObject("");
			obj3.getAttribute().setGroupPath("/C");
			dao.create(obj3);
			
			StoredAttribute obj4 = getObject("");
			obj4 = new StoredAttribute(obj4.getAttribute(), entityId2);
			dao.create(obj4);
			
			List<AttributeExt2> attributes = dao.getAttributes(null, 
					obj.getEntityId(), 
					null);
			
			assertAllAndOnlyAllIn(Lists.newArrayList(obj.getAttribute(), obj2.getAttribute(),
					obj3.getAttribute()), attributes);
		});
	}
	
	@Test
	public void allEntityAttributesWithNameAreReturned()
	{
		tx.runInTransaction(() -> {
			AttributeDAO dao = getDAO();
			StoredAttribute obj = getObject("");
			dao.create(obj);
			
			StoredAttribute obj2 = getObject("");
			obj2.getAttribute().setName("attr2");
			dao.create(obj2);
			
			StoredAttribute obj3 = getObject("");
			obj3.getAttribute().setGroupPath("/C");
			dao.create(obj3);
			
			List<AttributeExt2> attributes = dao.getAttributes(obj.getAttribute().getName(), 
					obj.getEntityId(), 
					null);
			
			assertAllAndOnlyAllIn(Lists.newArrayList(obj.getAttribute(), obj3.getAttribute()), 
					attributes);
		});
	}
	
	@Test
	public void specificEntityAttributeIsReturned()
	{
		tx.runInTransaction(() -> {
			AttributeDAO dao = getDAO();
			StoredAttribute obj = getObject("");
			dao.create(obj);
			
			List<AttributeExt2> attributes = dao.getAttributes(obj.getAttribute().getName(), 
					obj.getEntityId(), 
					obj.getAttribute().getGroupPath());
			
			assertAllAndOnlyAllIn(Lists.newArrayList(obj.getAttribute()), attributes);
		});
	}
	
	private void assertAllAndOnlyAllIn(List<AttributeExt2> expected, List<AttributeExt2> attributes)
	{
		assertThat(attributes.size(), is(expected.size()));
		Comparator<AttributeExt2> cmp = (a, b) -> {
			String n1 = a.getName() + "@" + a.getGroupPath();
			String n2 = b.getName() + "@" + b.getGroupPath();
			return n1.compareTo(n2);
		}; 
		Collections.sort(attributes, cmp);
		Collections.sort(expected, cmp);
		
		for (int i=0; i<attributes.size(); i++)
			assertThat(attributes.get(i), is(expected.get(i)));
	}
	
	
	@Test
	public void removedEntityAttributesInGroupAreNotReturned()
	{
		tx.runInTransaction(() -> {
			AttributeDAO dao = getDAO();
			StoredAttribute obj = getObject("");
			dao.create(obj);
			
			StoredAttribute obj2 = getObject("");
			obj2.getAttribute().setName("attr2");
			dao.create(obj2);

			StoredAttribute obj3 = getObject("");
			obj3.getAttribute().setGroupPath("/C");
			dao.create(obj3);
			
			dao.deleteAttributesInGroup(entityId, "/A");
			
			List<AttributeExt2> attributes = dao.getAttributes(null, entityId, null);
			
			assertAllAndOnlyAllIn(Lists.newArrayList(obj3.getAttribute()), 
					attributes);
		});
	}

	@Test
	public void removedAttributeIsNotReturned()
	{
		tx.runInTransaction(() -> {
			AttributeDAO dao = getDAO();
			StoredAttribute obj = getObject("");
			dao.create(obj);
			
			StoredAttribute obj2 = getObject("");
			obj2.getAttribute().setName("attr2");
			dao.create(obj2);

			StoredAttribute obj3 = getObject("");
			obj3.getAttribute().setGroupPath("/C");
			dao.create(obj3);
			
			dao.deleteAttribute("attr", entityId, "/A");
			
			List<AttributeExt2> attributes = dao.getAttributes(null, entityId, null);
			
			assertAllAndOnlyAllIn(Lists.newArrayList(obj2.getAttribute(), obj3.getAttribute()), 
					attributes);
		});
	}
	
	@Test
	public void updatedAttributeIsReturned()
	{
		tx.runInTransaction(() -> {
			AttributeDAO dao = getDAO();
			StoredAttribute obj = getObject("");
			dao.create(obj);

			mutateObject(obj);
			dao.updateAttribute(obj);

			List<AttributeExt2> attributes = dao.getAttributes("attr", entityId, "/A");

			assertAllAndOnlyAllIn(Lists.newArrayList(obj.getAttribute()), 
					attributes);
		});
	}
	
	@Test
	public void attributesAreRemovedWhenTypeIsRemoved()
	{
		tx.runInTransaction(() -> {
			AttributeDAO dao = getDAO();
			StoredAttribute obj = getObject("");
			dao.create(obj);
			
			StoredAttribute obj2 = getObject("");
			obj2.getAttribute().setName("attr2");
			dao.create(obj2);

			StoredAttribute obj3 = getObject("");
			obj3.getAttribute().setGroupPath("/C");
			dao.create(obj3);
			
			atDao.delete("attr");
			
			List<AttributeExt2> attributes = dao.getAttributes(null, entityId, null);
			
			assertAllAndOnlyAllIn(Lists.newArrayList(obj2.getAttribute()), 
					attributes);
		});
	}

	@Test
	public void attributesAreRemovedWheEntityIsRemoved()
	{
		tx.runInTransaction(() -> {
			AttributeDAO dao = getDAO();
			StoredAttribute obj = getObject("");
			dao.create(obj);
			
			StoredAttribute obj2 = getObject("");
			obj2.getAttribute().setName("attr2");
			dao.create(obj2);

			StoredAttribute obj3 = getObject("");
			obj3.getAttribute().setGroupPath("/C");
			dao.create(obj3);
			
			StoredAttribute obj4 = getObject("");
			obj4 = new StoredAttribute(obj4.getAttribute(), entityId2);
			dao.create(obj4);

			entityDAO.deleteByKey(entityId);
			
			List<AttributeExt2> attributes = dao.getAttributes(null, entityId, null);
			assertThat(attributes.isEmpty(), is(true));
			
			attributes = dao.getAttributes(null, entityId2, null);
			assertAllAndOnlyAllIn(Lists.newArrayList(obj4.getAttribute()), 
					attributes);
		});
	}
	
	@Test
	public void attributesAreRemovedWhenGroupIsRemoved()
	{
		tx.runInTransaction(() -> {
			AttributeDAO dao = getDAO();
			StoredAttribute obj = getObject("");
			dao.create(obj);
			
			StoredAttribute obj2 = getObject("");
			obj2.getAttribute().setName("attr2");
			dao.create(obj2);

			StoredAttribute obj3 = getObject("");
			obj3.getAttribute().setGroupPath("/C");
			dao.create(obj3);
			
			groupDAO.delete("/A");
			
			List<AttributeExt2> attributes = dao.getAttributes(null, entityId, null);
			
			assertAllAndOnlyAllIn(Lists.newArrayList(obj3.getAttribute()), 
					attributes);
		});
	}

	@Test
	public void attributeIsUpdatedAfterGroupRename()
	{
		tx.runInTransaction(() -> {
			AttributeDAO dao = getDAO();
			StoredAttribute obj = getObject("");
			dao.create(obj);
			
			long groupKey = groupDAO.getKeyForName("/A");
			groupDAO.updateByKey(groupKey, new Group("/ZZ"));
			
			List<AttributeExt2> attributes = dao.getAttributes(null, entityId, null);
			
			obj.getAttribute().setGroupPath("/ZZ");
			assertAllAndOnlyAllIn(Lists.newArrayList(obj.getAttribute()), 
					attributes);
		});
	}

	@Test
	public void attributeIsUpdatedAfterTypeRename()
	{
		tx.runInTransaction(() -> {
			AttributeDAO dao = getDAO();
			StoredAttribute obj = getObject("");
			dao.create(obj);
			
			long atKey = atDao.getKeyForName("attr");
			
			atDao.updateByKey(atKey, new AttributeType("attrZZ", "syntax"));
			
			List<AttributeExt2> attributes = dao.getAttributes(null, entityId, null);
			
			obj.getAttribute().setName("attrZZ");
			assertAllAndOnlyAllIn(Lists.newArrayList(obj.getAttribute()), 
					attributes);
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
		Attribute2 attr = new Attribute2("attr", 
				"syntax", 
				"/A", 
				Lists.newArrayList("v1", "v2"), 
				"remoteIdp", 
				"translationProfile");
		AttributeExt2 a = new AttributeExt2(attr, true, new Date(100), new Date(1000));
		return new StoredAttribute(a, entityId);
	}

	@Override
	protected void mutateObject(StoredAttribute src)
	{
		AttributeExt2 a = (AttributeExt2) src.getAttribute();
		a.setRemoteIdp("remoteIdp2");
		a.setTranslationProfile("translationProfile2");
		a.setValues(Lists.newArrayList("w1"));
		a.setUpdateTs(new Date(2000));
	}

	@Override
	protected void assertAreEqual(StoredAttribute obj, StoredAttribute cmp)
	{
		assertThat(obj, is(cmp));
	}
}
