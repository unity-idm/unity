/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import pl.edu.icm.unity.base.entity.EntityInformation;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.entity.Identity;
import pl.edu.icm.unity.base.entity.IdentityTaV;
import pl.edu.icm.unity.base.entity.IdentityType;
import pl.edu.icm.unity.engine.UnityIntegrationTest;
import pl.edu.icm.unity.engine.api.ServerManagement;
import pl.edu.icm.unity.engine.api.identity.EntityResolver;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.store.api.IdentityDAO;
import pl.edu.icm.unity.store.api.IdentityTypeDAO;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.store.types.StoredIdentity;

@RunWith(SpringJUnit4ClassRunner.class)
@UnityIntegrationTest
public class EntityResolverImplTest
{
	@Autowired
	protected TransactionalRunner tx;
	
	@Autowired
	private EntityResolver entityResolver;
	
	@Autowired
	private EntityDAO entDAO;
	
	@Autowired
	private IdentityDAO identityDAO;

	@Autowired
	private IdentityTypeDAO itDAO;
	
	@Autowired
	@Qualifier("insecure")
	protected ServerManagement insecureServerMan;
	
	@Before
	public void clean() throws Exception
	{
		insecureServerMan.resetDatabase();
		tx.runInTransaction(() -> {
			itDAO.deleteAll();
		});
	}

	@Test
	public void shouldFailOnResolvingMissingEPWithId() throws Exception
	{
		tx.runInTransactionThrowing(() -> {
			Throwable error = catchThrowable(() -> entityResolver.getEntityId(new EntityParam(12345l)));
			
			assertThat(error).isInstanceOf(IllegalArgumentException.class);
		});
	}
	@Test
	public void shouldFailOnResolvingMissingEPWithTaV() throws Exception
	{
		tx.runInTransactionThrowing(() -> {
			Throwable error = catchThrowable(() -> entityResolver.getEntityId(new EntityParam(new IdentityTaV("userName", "missing"))));
			
			assertThat(error).isInstanceOf(IllegalArgumentException.class);
		});
	}
	@Test
	public void shouldFailOnResolvingMissingTaV() throws Exception
	{
		tx.runInTransactionThrowing(() -> {
			Throwable error = catchThrowable(() -> entityResolver.getEntityId(new IdentityTaV("userName", "missing")));
			
			assertThat(error).isInstanceOf(IllegalArgumentException.class);
		});
	}
	@Test
	public void shouldResolveExistingEPWithId() throws Exception
	{
		tx.runInTransactionThrowing(() -> {
			long entity = entDAO.create(new EntityInformation());
			long ret = entityResolver.getEntityId(new EntityParam(entity));
			
			assertThat(ret, is(entity));
		});
	}
	@Test
	public void shouldResolveExistingEPWithTaV() throws Exception
	{
		tx.runInTransactionThrowing(() -> {
			long entity = entDAO.create(new EntityInformation());
			Identity obj = new Identity(UsernameIdentity.ID, "name1", entity, "name1");
			itDAO.create(new IdentityType(UsernameIdentity.ID, UsernameIdentity.ID));
			identityDAO.create(new StoredIdentity(obj));
		
			long ret = entityResolver.getEntityId(new EntityParam(new IdentityTaV("userName", obj.getName())));
			
			assertThat(ret, is(entity));
		});
	}
	@Test
	public void shouldResolveExistingTaV() throws Exception
	{
		tx.runInTransactionThrowing(() -> {
			long entity = entDAO.create(new EntityInformation());
			Identity obj = new Identity(UsernameIdentity.ID, "name1", entity, "name1");
			itDAO.create(new IdentityType(UsernameIdentity.ID, UsernameIdentity.ID));
			identityDAO.create(new StoredIdentity(obj));

			long ret = entityResolver.getEntityId(new IdentityTaV("userName", obj.getName()));
			
			assertThat(ret, is(entity));
		});
	}
}
