/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.identity;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import pl.edu.icm.unity.engine.api.identity.EntityResolver;
import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.store.api.IdentityDAO;
import pl.edu.icm.unity.store.api.IdentityTypeDAO;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.types.basic.EntityInformation;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import pl.edu.icm.unity.types.basic.IdentityType;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath*:META-INF/components.xml"})
@ActiveProfiles("test")
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

	@Before
	public void clean()
	{
		tx.runInTransaction(() -> {
			itDAO.deleteAll();
		});
	}
	
	@Test
	public void shouldFailOnResolvingMissingEPWithId() throws Exception
	{
		tx.runInTransactionThrowing(() -> {
			catchException(entityResolver).getEntityId(new EntityParam(12345l));
			
			assertThat(caughtException(), isA(IllegalArgumentException.class));
		});
	}
	@Test
	public void shouldFailOnResolvingMissingEPWithTaV() throws Exception
	{
		tx.runInTransactionThrowing(() -> {
			catchException(entityResolver).getEntityId(new EntityParam(new IdentityTaV("userName", "missing")));
			
			assertThat(caughtException(), isA(IllegalArgumentException.class));
		});
	}
	@Test
	public void shouldFailOnResolvingMissingTaV() throws Exception
	{
		tx.runInTransactionThrowing(() -> {
			catchException(entityResolver).getEntityId(new IdentityTaV("userName", "missing"));
			
			assertThat(caughtException(), isA(IllegalArgumentException.class));
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
			Identity obj = new Identity("userName", "name1", entity, "name1");
			itDAO.create(new IdentityType("userName"));
			identityDAO.create(obj);
		
			long ret = entityResolver.getEntityId(new EntityParam(new IdentityTaV("userName", obj.getName())));
			
			assertThat(ret, is(entity));
		});
	}
	@Test
	public void shouldResolveExistingTaV() throws Exception
	{
		tx.runInTransactionThrowing(() -> {
			long entity = entDAO.create(new EntityInformation());
			Identity obj = new Identity("userName", "name1", entity, "name1");
			itDAO.create(new IdentityType("userName"));
			identityDAO.create(obj);

			long ret = entityResolver.getEntityId(new IdentityTaV("userName", obj.getName()));
			
			assertThat(ret, is(entity));
		});
	}
}
