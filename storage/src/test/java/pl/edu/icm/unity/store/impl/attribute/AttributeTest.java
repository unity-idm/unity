/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attribute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.MembershipDAO;
import pl.edu.icm.unity.store.impl.AbstractBasicDAOTest;
import pl.edu.icm.unity.store.impl.StorageLimits.SizeLimitExceededException;
import pl.edu.icm.unity.store.types.StoredAttribute;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.EntityInformation;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupMembership;

public class AttributeTest extends AbstractBasicDAOTest<StoredAttribute>
{
	@Autowired
	private AttributeDAO dao;
	@Autowired
	private GroupDAO groupDAO;
	@Autowired
	private EntityDAO entityDAO;
	@Autowired
	private AttributeTypeDAO atDao;
	@Autowired
	private MembershipDAO membershipDao;

	
	private long entityId;
	private long entityId2;

	@Before
	public void cleanDB()
	{
		dbCleaner.reset();
		tx.runInTransaction(() -> {
			entityId = entityDAO.create(new EntityInformation());
			entityId2 = entityDAO.create(new EntityInformation());
			groupDAO.create(new Group("/A"));
			groupDAO.create(new Group("/B"));
			groupDAO.create(new Group("/C"));
			atDao.create(new AttributeType("attr", "syntax"));
			atDao.create(new AttributeType("attr2", "syntax"));
			atDao.create(new AttributeType("attr3", "syntax"));
		});
	}

	@Test
	public void allAttributesByNameAreReturned()
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
			
			List<StoredAttribute> attributes = dao.getAttributes("attr", null, null);
			
			assertAllAndOnlyAllInSA(Lists.newArrayList(obj, obj3, obj4), attributes);
		});
	}

	@Test
	public void allAttributesByGroupAreReturned()
	{
		tx.runInTransaction(() -> {
			AttributeDAO dao = getDAO();
			StoredAttribute obj = getObject("");
			dao.create(obj);
			
			StoredAttribute obj2 = getObject("");
			obj2.getAttribute().setGroupPath("/C");
			obj2.getAttribute().setName("attr2");
			dao.create(obj2);
			
			StoredAttribute obj3 = getObject("");
			obj3.getAttribute().setGroupPath("/C");
			dao.create(obj3);
			
			StoredAttribute obj4 = getObject("");
			obj4 = new StoredAttribute(obj4.getAttribute(), entityId2);
			obj4.getAttribute().setGroupPath("/C");
			dao.create(obj4);
			
			List<StoredAttribute> attributes = dao.getAttributes(null, null, "/C");
			
			assertAllAndOnlyAllInSA(Lists.newArrayList(obj2, obj3, obj4), attributes);
		});
	}

	@Test
	public void allAttribtuesOfGroupMembersAreReturned()
	{
		tx.runInTransaction(() -> {
			AttributeDAO dao = getDAO();
			StoredAttribute obj = getObject("");
			obj.getAttribute().setGroupPath("/");
			obj.getAttribute().setName("attr");
			dao.create(obj);
			
			StoredAttribute obj2 = getObject("");
			obj2.getAttribute().setGroupPath("/C");
			obj2.getAttribute().setName("attr2");
			dao.create(obj2);

			StoredAttribute obj4 = getObject("");
			obj4.getAttribute().setGroupPath("/C");
			obj4.getAttribute().setName("attr3");
			obj4 = new StoredAttribute(obj4.getAttribute(), entityId2);
			dao.create(obj4);


			membershipDao.create(new GroupMembership("/C", entityId, new Date(1)));
			membershipDao.create(new GroupMembership("/", entityId, new Date(1)));
			membershipDao.create(new GroupMembership("/", entityId2, new Date(1)));
			
			List<StoredAttribute> attributes = dao.getAttributesOfGroupMembers("/C");
			
			assertAllAndOnlyAllInSA(Lists.newArrayList(obj, obj2), attributes);
		});
	}
	
	@Test
	public void allAttributesByNameAndGroupAreReturned()
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
			
			List<StoredAttribute> attributes = dao.getAttributes("attr", null, "/A");
			
			assertAllAndOnlyAllInSA(Lists.newArrayList(obj, obj4), attributes);
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
			
			List<AttributeExt> attributes = dao.getEntityAttributes(obj.getEntityId(), null, "/A");
			
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
			
			List<AttributeExt> attributes = dao.getAllEntityAttributes(obj.getEntityId());
			
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
			
			List<AttributeExt> attributes = dao.getEntityAttributes(obj.getEntityId(), 
					obj.getAttribute().getName(), 
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
			
			List<AttributeExt> attributes = dao.getEntityAttributes(obj.getEntityId(), 
					obj.getAttribute().getName(), 
					obj.getAttribute().getGroupPath());
			
			assertAllAndOnlyAllIn(Lists.newArrayList(obj.getAttribute()), attributes);
		});
	}
	
	private void assertAllAndOnlyAllIn(List<AttributeExt> expected, List<AttributeExt> attributes)
	{
		assertThat(attributes.size(), is(expected.size()));
		Comparator<AttributeExt> cmp = (a, b) -> {
			String n1 = a.getName() + "@" + a.getGroupPath();
			String n2 = b.getName() + "@" + b.getGroupPath();
			return n1.compareTo(n2);
		}; 
		Collections.sort(attributes, cmp);
		Collections.sort(expected, cmp);
		
		for (int i=0; i<attributes.size(); i++)
			assertThat(attributes.get(i), is(expected.get(i)));
	}
	
	private void assertAllAndOnlyAllInSA(List<StoredAttribute> expected, List<StoredAttribute> attributes)
	{
		assertThat(attributes.size(), is(expected.size()));
		Comparator<StoredAttribute> cmp = (a, b) -> {
			String n1 = a.getAttribute().getName() + "@" + a.getAttribute().getGroupPath() + "#" + 
					a.getEntityId();
			String n2 = b.getAttribute().getName() + "@" + b.getAttribute().getGroupPath() + "#" +
					b.getEntityId();
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
			
			List<AttributeExt> attributes = dao.getAllEntityAttributes(entityId);
			
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
			
			List<AttributeExt> attributes = dao.getAllEntityAttributes(entityId);
			
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

			List<AttributeExt> attributes = dao.getEntityAttributes(entityId, "attr", "/A");

			assertAllAndOnlyAllIn(Lists.newArrayList(obj.getAttribute()), 
					attributes);
		});
	}
	
	@Test
	public void updated2ndAttributeIsReturned()
	{
		tx.runInTransaction(() -> {
			AttributeDAO dao = getDAO();
			StoredAttribute obj = getObject("");
			dao.create(obj);
			Attribute attr = new Attribute("attr2", 
					"syntax", 
					"/A", 
					Lists.newArrayList("v1", "v2"), 
					"remoteIdp", 
					"translationProfile");
			AttributeExt a = new AttributeExt(attr, true, new Date(100), new Date(1000));
			StoredAttribute obj2 = new StoredAttribute(a, entityId);
			dao.create(obj2);

			mutateObject(obj2);
			dao.updateAttribute(obj2);

			List<AttributeExt> attributes = dao.getEntityAttributes(entityId, "attr", "/A");

			assertAllAndOnlyAllIn(Lists.newArrayList(obj.getAttribute()), 
					attributes);

			List<AttributeExt> attributes2 = dao.getEntityAttributes(entityId, "attr2", "/A");

			assertAllAndOnlyAllIn(Lists.newArrayList(obj2.getAttribute()), 
					attributes2);
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
			
			List<AttributeExt> attributes = dao.getAllEntityAttributes(entityId);
			
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
			
			List<AttributeExt> attributes = dao.getAllEntityAttributes(entityId);
			assertThat(attributes.isEmpty(), is(true));
			
			attributes = dao.getAllEntityAttributes(entityId2);
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
			
			List<AttributeExt> attributes = dao.getAllEntityAttributes(entityId);
			
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
			
			List<AttributeExt> attributes = dao.getAllEntityAttributes(entityId);
			
			StoredAttribute clone = new StoredAttribute(obj);
			clone.getAttribute().setGroupPath("/ZZ");
			assertAllAndOnlyAllIn(Lists.newArrayList(clone.getAttribute()), 
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
			
			List<AttributeExt> attributes = dao.getAllEntityAttributes(entityId);
			
			StoredAttribute clone = new StoredAttribute(obj);
			clone.getAttribute().setName("attrZZ");
			assertAllAndOnlyAllIn(Lists.newArrayList(clone.getAttribute()), 
					attributes);
		});
	}

	@Test
	public void attributeSizeLimitIsInEffect()
	{
		tx.runInTransaction(() -> {
			AttributeDAO dao = getDAO();
			StoredAttribute obj = getSizedAttr(256001);
			
			Throwable error = catchThrowable(() -> dao.create(obj));

			assertThat(error).isInstanceOf(SizeLimitExceededException.class);
		});
	}

	@Test
	public void attributeFittingLimitCanBeAdded()
	{
		tx.runInTransaction(() -> {
			AttributeDAO dao = getDAO();
			StoredAttribute obj = getSizedAttr(250000);
			
			Throwable error = catchThrowable(() -> dao.create(obj));

			assertThat(error).isNull();
		});
	}

	
	private StoredAttribute getSizedAttr(int size)
	{
		StringBuilder value = new StringBuilder(size);
		IntStream.range(0, size).forEach(i -> value.append("."));
		Attribute attr = new Attribute("attr", 
				"syntax", 
				"/A", 
				Lists.newArrayList(value.toString()), 
				"remoteIdp", 
				"translationProfile");
		AttributeExt a = new AttributeExt(attr, true, new Date(100), new Date(1000));
		return new StoredAttribute(a, entityId);
	}

	@Override
	protected AttributeDAO getDAO()
	{
		return dao;
	}

	@Override
	protected StoredAttribute getObject(String id)
	{
		Attribute attr = new Attribute("attr", 
				"syntax", 
				"/A", 
				Lists.newArrayList("v1", "v2"), 
				"remoteIdp", 
				"translationProfile");
		AttributeExt a = new AttributeExt(attr, true, new Date(100), new Date(1000));
		return new StoredAttribute(a, entityId);
	}

	@Override
	protected StoredAttribute mutateObject(StoredAttribute src)
	{
		AttributeExt a = (AttributeExt) src.getAttribute();
		a.setRemoteIdp("remoteIdp2");
		a.setTranslationProfile("translationProfile2");
		a.setValues(Lists.newArrayList("w1"));
		a.setUpdateTs(new Date(2000));
		return src;
	}
}
