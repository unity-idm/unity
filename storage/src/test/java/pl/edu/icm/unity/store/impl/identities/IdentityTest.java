/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.identities;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.entity.EntityInformation;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupMembership;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.identity.IdentityType;
import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.IdentityDAO;
import pl.edu.icm.unity.store.api.IdentityTypeDAO;
import pl.edu.icm.unity.store.api.MembershipDAO;
import pl.edu.icm.unity.store.api.NamedCRUDDAO;
import pl.edu.icm.unity.store.impl.AbstractNamedDAOTest;
import pl.edu.icm.unity.store.types.StoredIdentity;

public class IdentityTest extends AbstractNamedDAOTest<StoredIdentity>
{
	@Autowired
	private IdentityDAO dao;
	
	@Autowired
	private IdentityTypeDAO itDao;

	@Autowired
	private EntityDAO entDao;
	
	@Autowired
	private MembershipDAO membershipDao;
	
	@Autowired
	private GroupDAO groupDAO;
	
	private long entity;
	private long entity2;
	
	@BeforeEach
	public void createReferenced()
	{
		tx.runInTransaction(() -> {
			itDao.create(new IdentityType("username"));
			entity = entDao.create(new EntityInformation());
			entity2 = entDao.create(new EntityInformation());
		});
	}
	
	@Test
	public void allIdentitiesAreRemovedWhenTheirEntityIsRemoved()
	{
		tx.runInTransaction(() -> {
			StoredIdentity obj = getObject("name1");
			dao.create(obj);
			StoredIdentity obj2 = getObject("name2");
			dao.create(obj2);
			
			entDao.deleteByKey(entity);
			
			List<StoredIdentity> ret = dao.getAll();

			assertThat(ret).isNotNull();
			assertThat(ret).isEmpty();
		});
	}	
	
	@Test
	public void shouldReturnAllCreatedByEntity()
	{
		tx.runInTransaction(() -> {
			StoredIdentity obj = getObject("name1");
			dao.create(obj);
			StoredIdentity obj2 = getObject("name2");
			dao.create(obj2);
			
			List<StoredIdentity> ret = dao.getByEntityFull(entity);

			assertThat(ret).isNotNull();
			assertThat(ret).hasSize(2);
			if (ret.get(0).getIdentity().getValue().equals("name2"))
			{
				StoredIdentity tmp = ret.get(0);
				ret.add(0, ret.get(1));
				ret.add(1, tmp);
			}
			
			assertThat(obj).isEqualTo(ret.get(0));
			assertThat(obj2).isEqualTo(ret.get(1));
		});
	}

	@Test
	public void shouldReturnByGroupMembership()
	{
		tx.runInTransaction(() -> {
			StoredIdentity obj = getObject("name1");
			dao.create(obj);
			StoredIdentity obj2 = getObject("name2");
			dao.create(obj2);
			
			StoredIdentity obj3 = getObject("name3");
			obj3.getIdentity().setEntityId(entity2);
			dao.create(obj3);
			
			groupDAO.create(new Group("/C"));
			groupDAO.create(new Group("/A"));
			membershipDao.create(new GroupMembership("/C", entity, new Date(1)));
			membershipDao.create(new GroupMembership("/A", entity2, new Date(1)));
			
			List<StoredIdentity> ret = dao.getByGroup("/C");

			assertThat(ret).isNotNull();
			assertThat(ret).hasSize(2);
			if (ret.get(0).getIdentity().getValue().equals("name2"))
			{
				StoredIdentity tmp = ret.get(0);
				ret.add(0, ret.get(1));
				ret.add(1, tmp);
			}

			assertThat(obj).isEqualTo(ret.get(0));
			assertThat(obj2).isEqualTo(ret.get(1));
		});
	}
	
	@Test
	public void shouldReturnByTypeAndValues()
	{
		tx.runInTransaction(() -> {
			StoredIdentity obj = getObject("name1");
			long id1 = dao.create(obj);
			StoredIdentity obj2 = getObject("name2");
			obj2.getIdentity().setEntityId(entity2);
			long id2 = dao.create(obj2);
			
			Set<Long> idByTypeAndValues = dao.getIdByTypeAndValues("username", List.of("name1", "name2"));
			
			assertThat(idByTypeAndValues.size()).isEqualTo(2);
			assertThat(idByTypeAndValues).contains(id1, id2);
		});
	}
	
	@Override
	@Test
	public void shouldFailOnCreatingWithTooLongName()
	{
		//this test is skipped: we do allow for creating identities with very long names.
	}
	
	@Test
	public void insertedListIsReturned()
	{
		super.insertedListIsReturned();
	}
	
	@Override
	protected NamedCRUDDAO<StoredIdentity> getDAO()
	{
		return dao;
	}

	@Override
	protected StoredIdentity getObject(String name)
	{
		ObjectNode meta = Constants.MAPPER.createObjectNode();
		Identity ret = new Identity("username", name, entity, name);
		ret.setCreationTs(new Date(100324));
		ret.setMetadata(meta);
		ret.setRealm("realm");
		ret.setRemoteIdp("remoteIdp");
		ret.setTarget("target");
		ret.setTranslationProfile("translationProfile");
		ret.setUpdateTs(new Date(100));
		ret.setCreationTs(new Date(101));
		return new StoredIdentity(ret);
	}

	@Override
	protected StoredIdentity mutateObject(StoredIdentity sret)
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode meta = mapper.createObjectNode();
		meta.put("1", "v");
		
		Identity ret = sret.getIdentity();
		ret.setCreationTs(new Date(100324));
		ret.setEntityId(entity2);
		ret.setMetadata(meta);
		ret.setRealm("realm2");
		ret.setRemoteIdp("remoteIdp2");
		ret.setTarget("target2");
		ret.setTranslationProfile("translationProfile2");
		ret.setUpdateTs(new Date(100444));
		return sret;
	}
}
