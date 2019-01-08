/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.store.StorageCleanerImpl;
import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.MembershipDAO;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.store.types.StoredAttribute;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.EntityInformation;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupMembership;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath*:META-INF/components.xml"})
public class CachingIntegrityTest
{
	@Autowired
	private GroupDAO groupDAO;
	@Autowired
	private MembershipDAO membershipDAO;
	@Autowired
	private AttributeDAO attributeDAO;
	@Autowired
	private AttributeTypeDAO attributeTypeDAO;
	@Autowired 
	private EntityDAO entityDAO;
	@Autowired
	private StorageCleanerImpl dbCleaner;
	@Autowired
	private TransactionalRunner tx;
	
	@Before
	public void cleanDB()
	{
		dbCleaner.reset();
	}

	
	@After
	public void shutdown()
	{
		dbCleaner.shutdown();
	}
	
	@Ignore("Ignored as currently attributes can be set in a group even without user membership, "
			+ "and also membership removal is not removing attribute. "
			+ "Although this may be changed in future so the test is here")
	@Test
	public void shouldFlushAttributeCacheAfterMembershipChange()
	{
		tx.runInTransaction(() -> 
		{
			//given
			long id = entityDAO.create(new EntityInformation());
			groupDAO.create(new Group("/A"));
			membershipDAO.create(new GroupMembership("/A", id, new Date()));
			attributeTypeDAO.create(new AttributeType("name", "syntax"));
			long attributeId = attributeDAO.create(new StoredAttribute(
					new AttributeExt(new Attribute("name", "syntax", "/A", Lists.newArrayList()), true), id));

			//fill cache
			List<AttributeExt> allEntityAttributes = attributeDAO.getAllEntityAttributes(id);
			assertThat(allEntityAttributes.size(), is(1));

			//when
			membershipDAO.deleteByKey(id, "/A");

			//then
			allEntityAttributes = attributeDAO.getAllEntityAttributes(id);
			assertThat(allEntityAttributes.isEmpty(), is(true));
			Throwable error = catchThrowable(() -> attributeDAO.getByKey(attributeId));
			assertThat(error).isInstanceOf(IllegalArgumentException.class);
		});
	}

	@Test
	public void shouldFlushMembershipCacheAfterGroupsChange()
	{
		tx.runInTransaction(() -> 
		{
			//given
			long id = entityDAO.create(new EntityInformation());
			groupDAO.create(new Group("/A"));
			membershipDAO.create(new GroupMembership("/A", id, new Date()));

			//fill cache
			List<GroupMembership> all = membershipDAO.getAll();
			assertThat(all.size(), is(1));

			//when
			groupDAO.delete("/A");

			//then
			all = membershipDAO.getAll();
			assertThat(all.isEmpty(), is(true));
			List<GroupMembership> entityMembership = membershipDAO.getEntityMembership(id);
			assertThat(entityMembership.isEmpty(), is(true));
		});
	}

	@Test
	public void shouldFlushAttributeCacheAfterGroupChange()
	{
		tx.runInTransaction(() -> 
		{
			//given
			long id = entityDAO.create(new EntityInformation());
			groupDAO.create(new Group("/A"));
			membershipDAO.create(new GroupMembership("/A", id, new Date()));
			attributeTypeDAO.create(new AttributeType("name", "syntax"));
			long attributeId = attributeDAO.create(new StoredAttribute(
					new AttributeExt(new Attribute("name", "syntax", "/A", Lists.newArrayList()), true), id));

			//fill cache
			List<AttributeExt> allEntityAttributes = attributeDAO.getAllEntityAttributes(id);
			assertThat(allEntityAttributes.size(), is(1));

			//when
			groupDAO.delete("/A");

			//then
			allEntityAttributes = attributeDAO.getAllEntityAttributes(id);
			assertThat(allEntityAttributes.isEmpty(), is(true));
			Throwable error = catchThrowable(() -> attributeDAO.getByKey(attributeId));
			assertThat(error).isInstanceOf(IllegalArgumentException.class);
		});
	}
	
	@Test
	public void shouldFlushAttributeCacheAfterAttributeTypeChange()
	{
		tx.runInTransaction(() -> 
		{
			//given
			long id = entityDAO.create(new EntityInformation());
			groupDAO.create(new Group("/A"));
			membershipDAO.create(new GroupMembership("/A", id, new Date()));
			attributeTypeDAO.create(new AttributeType("name", "syntax"));
			long attributeId = attributeDAO.create(new StoredAttribute(
					new AttributeExt(new Attribute("name", "syntax", "/A", Lists.newArrayList()), true), id));

			//fill cache
			List<AttributeExt> allEntityAttributes = attributeDAO.getAllEntityAttributes(id);
			assertThat(allEntityAttributes.size(), is(1));

			//when
			attributeTypeDAO.deleteAll();

			//then
			allEntityAttributes = attributeDAO.getAllEntityAttributes(id);
			assertThat(allEntityAttributes.isEmpty(), is(true));
			Throwable error = catchThrowable(() -> attributeDAO.getByKey(attributeId));
			assertThat(error).isInstanceOf(IllegalArgumentException.class);
		});
	}
	
	@Test
	public void shouldFlushAttributeCacheAfterEntityChange()
	{
		tx.runInTransaction(() -> 
		{
			//given
			long id = entityDAO.create(new EntityInformation());
			groupDAO.create(new Group("/A"));
			membershipDAO.create(new GroupMembership("/A", id, new Date()));
			attributeTypeDAO.create(new AttributeType("name", "syntax"));
			long attributeId = attributeDAO.create(new StoredAttribute(
					new AttributeExt(new Attribute("name", "syntax", "/A", Lists.newArrayList()), true), id));

			//fill cache
			List<AttributeExt> allEntityAttributes = attributeDAO.getAllEntityAttributes(id);
			assertThat(allEntityAttributes.size(), is(1));

			//when
			entityDAO.deleteByKey(id);

			//then
			allEntityAttributes = attributeDAO.getAllEntityAttributes(id);
			assertThat(allEntityAttributes.isEmpty(), is(true));
			Throwable error = catchThrowable(() -> attributeDAO.getByKey(attributeId));
			assertThat(error).isInstanceOf(IllegalArgumentException.class);
		});
	}
}
