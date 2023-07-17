/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.entity.EntityInformation;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupMembership;
import pl.edu.icm.unity.store.StorageCleanerImpl;
import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.MembershipDAO;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.store.types.StoredAttribute;

@ExtendWith(SpringExtension.class)
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
	
	@BeforeEach
	public void cleanDB()
	{
		dbCleaner.cleanOrDelete();
	}

	
	@AfterEach
	public void shutdown()
	{
		dbCleaner.shutdown();
	}
	
	@Disabled("Ignored as currently attributes can be set in a group even without user membership, "
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
			assertThat(allEntityAttributes.size()).isEqualTo(1);

			//when
			membershipDAO.deleteByKey(id, "/A");

			//then
			allEntityAttributes = attributeDAO.getAllEntityAttributes(id);
			assertThat(allEntityAttributes.isEmpty()).isTrue();
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
			assertThat(all).hasSize(1);

			//when
			groupDAO.delete("/A");

			//then
			all = membershipDAO.getAll();
			assertThat(all.isEmpty()).isTrue();
			List<GroupMembership> entityMembership = membershipDAO.getEntityMembership(id);
			assertThat(entityMembership.isEmpty()).isTrue();
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
			assertThat(allEntityAttributes).hasSize(1);

			//when
			groupDAO.delete("/A");

			//then
			allEntityAttributes = attributeDAO.getAllEntityAttributes(id);
			assertThat(allEntityAttributes).isEmpty();
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
			assertThat(allEntityAttributes).hasSize(1);

			//when
			attributeTypeDAO.deleteAll();

			//then
			allEntityAttributes = attributeDAO.getAllEntityAttributes(id);
			assertThat(allEntityAttributes).isEmpty();;
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
			assertThat(allEntityAttributes).hasSize(1);

			//when
			entityDAO.deleteByKey(id);

			//then
			allEntityAttributes = attributeDAO.getAllEntityAttributes(id);
			assertThat(allEntityAttributes).isEmpty();;
			Throwable error = catchThrowable(() -> attributeDAO.getByKey(attributeId));
			assertThat(error).isInstanceOf(IllegalArgumentException.class);
		});
	}
}
