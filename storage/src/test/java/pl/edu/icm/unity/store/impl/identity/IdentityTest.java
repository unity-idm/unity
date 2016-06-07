/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.identity;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.store.api.IdentityDAO;
import pl.edu.icm.unity.store.api.IdentityTypeDAO;
import pl.edu.icm.unity.store.api.NamedCRUDDAO;
import pl.edu.icm.unity.store.impl.AbstractNamedDAOTest;
import pl.edu.icm.unity.types.basic.EntityInformation;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityType;

public class IdentityTest extends AbstractNamedDAOTest<Identity>
{
	@Autowired
	private IdentityDAO dao;
	
	@Autowired
	private IdentityTypeDAO itDao;

	@Autowired
	private EntityDAO entDao;

	private long entity;
	private long entity2;
	
	@Before
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
			Identity obj = getObject("name1");
			dao.create(obj);
			Identity obj2 = getObject("name2");
			dao.create(obj2);
			
			entDao.deleteByKey(entity);
			
			List<Identity> ret = dao.getAll();

			assertThat(ret, is(notNullValue()));
			assertThat(ret.isEmpty(), is(true));
		});
	}	
	
	@Test
	public void shouldReturnAllCreatedByEntity()
	{
		tx.runInTransaction(() -> {
			Identity obj = getObject("name1");
			dao.create(obj);
			Identity obj2 = getObject("name2");
			dao.create(obj2);
			
			List<Identity> ret = dao.getByEntity(entity);

			assertThat(ret, is(notNullValue()));
			assertThat(ret.size(), is(2));
			if (ret.get(0).getValue().equals("name2"))
			{
				Identity tmp = ret.get(0);
				ret.add(0, ret.get(1));
				ret.add(1, tmp);
			}
			
			assertEquals(obj, ret.get(0));
			assertEquals(obj2, ret.get(1));
		});
	}
	
	@Override
	protected NamedCRUDDAO<Identity> getDAO()
	{
		return dao;
	}

	@Override
	protected Identity getObject(String name)
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
		return ret;
	}

	@Override
	protected Identity mutateObject(Identity ret)
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode meta = mapper.createObjectNode();
		meta.put("1", "v");
		
		ret.setCreationTs(new Date(100324));
		ret.setEntityId(entity2);
		ret.setMetadata(meta);
		ret.setRealm("realm2");
		ret.setRemoteIdp("remoteIdp2");
		ret.setTarget("target2");
		ret.setTranslationProfile("translationProfile2");
		ret.setUpdateTs(new Date(100444));
		return ret;
	}
}
