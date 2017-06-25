/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.hz;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import pl.edu.icm.unity.store.StorageCleanerImpl;
import pl.edu.icm.unity.store.StorageConfiguration;
import pl.edu.icm.unity.store.StorageEngine;
import pl.edu.icm.unity.store.api.IdentityTypeDAO;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.store.impl.identitytype.IdentityTypeRDBMSStore;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionalRunner;
import pl.edu.icm.unity.types.basic.IdentityType;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath*:META-INF/components.xml"})
public class HzRDBMSFlushTest
{
	@Autowired
	protected StorageCleanerImpl dbCleaner;
	@Autowired
	private IdentityTypeDAO idTypeDAO;
	@Autowired
	private IdentityTypeRDBMSStore idTypeRDBMSDAO;
	@Autowired @Qualifier(SQLTransactionalRunner.NAME)
	private TransactionalRunner rdbmsTx;
	@Autowired
	protected TransactionalRunner tx;
	@Autowired
	private HzStoreLoader hzLoader;
	
	@Autowired
	private StorageConfiguration systemCfg;
	
	@Before
	public void cleanDB()
	{
		StorageEngine engine = systemCfg.getEnumValue(StorageConfiguration.ENGINE, StorageEngine.class);
		Assume.assumeTrue(engine == StorageEngine.hz);
		
		dbCleaner.reset();
		
		
		//let's mess with the RDBMS primary keys for the table which will be used during tests.
		rdbmsTx.runInTransaction(() -> {
			IdentityType tst1 = getIdentityType("tst1");
			IdentityType tst2 = getIdentityType("tst2");
			idTypeRDBMSDAO.create(tst1);
			idTypeRDBMSDAO.create(tst2);
			idTypeRDBMSDAO.deleteAll();
		});
	}

	@Test
	public void shouldRestoreStoredObjects()
	{
		final int NUM = 100;
		List<IdentityType> idTypes =  new ArrayList<>(); 
		for (int i=0; i<NUM; i++)
			idTypes.add(getIdentityType("idType" + i));
		for (int i=0; i<NUM; i++)
		{
			IdentityType identityType = idTypes.get(i);
			tx.runInTransaction(() -> {
				idTypeDAO.create(identityType);
			});
			try { Thread.sleep(10); } catch (InterruptedException e) {}
		}

		tx.runInTransaction(() -> {
			hzLoader.reloadHzFromRDBMS();
		});
		
		tx.runInTransaction(() -> {
			List<IdentityType> all = idTypeDAO.getAll();
			assertThat(all, is(notNullValue()));
			assertThat(all.size(), is(NUM));
			for (int i=0; i<NUM; i++)
				assertThat(all, hasItem(idTypes.get(i)));
		});
	}
	
	@Test
	public void shouldCreateAndUpdateObject()
	{
		IdentityType identityType = getIdentityType("idType");
		IdentityType identityTypeUpdated = getIdentityType("idType");
		identityTypeUpdated.setDescription("d2");
		tx.runInTransaction(() -> {
			long id = idTypeDAO.create(identityType);
			idTypeDAO.updateByKey(id, identityTypeUpdated);
		});

		tx.runInTransaction(() -> {
			hzLoader.reloadHzFromRDBMS();
		});
		
		tx.runInTransaction(() -> {
			List<IdentityType> all = idTypeDAO.getAll();
			assertThat(all, is(notNullValue()));
			assertThat(all.size(), is(1));
			assertThat(all.get(0), is(identityTypeUpdated));
		});
	}

	@Test
	public void shouldCreateAndDeleteObject()
	{
		IdentityType identityType = getIdentityType("idType");
		IdentityType identityTypeUpdated = getIdentityType("idType");
		identityTypeUpdated.setDescription("d2");
		tx.runInTransaction(() -> {
			long id = idTypeDAO.create(identityType);
			idTypeDAO.deleteByKey(id);
		});

		tx.runInTransaction(() -> {
			hzLoader.reloadHzFromRDBMS();
		});
		
		tx.runInTransaction(() -> {
			List<IdentityType> all = idTypeDAO.getAll();
			assertThat(all, is(notNullValue()));
			assertThat(all.size(), is(0));
		});	
	}
	

	@Test
	public void shouldCreateAndDeleteObjectByName()
	{
		IdentityType identityType = getIdentityType("idType");
		IdentityType identityTypeUpdated = getIdentityType("idType");
		identityTypeUpdated.setDescription("d2");
		tx.runInTransaction(() -> {
			idTypeDAO.create(identityType);
			idTypeDAO.delete(identityType.getName());
		});

		tx.runInTransaction(() -> {
			hzLoader.reloadHzFromRDBMS();
		});
		
		tx.runInTransaction(() -> {
			List<IdentityType> all = idTypeDAO.getAll();
			assertThat(all, is(notNullValue()));
			assertThat(all.size(), is(0));
		});	
	}

	@Test
	public void shouldCreateAndUpdateObjectByName()
	{
		IdentityType identityType = getIdentityType("idType");
		IdentityType identityTypeUpdated = getIdentityType("idType");
		identityTypeUpdated.setDescription("d2");
		tx.runInTransaction(() -> {
			idTypeDAO.create(identityType);
			idTypeDAO.update(identityTypeUpdated);
		});

		tx.runInTransaction(() -> {
			hzLoader.reloadHzFromRDBMS();
		});
		
		tx.runInTransaction(() -> {
			List<IdentityType> all = idTypeDAO.getAll();
			assertThat(all, is(notNullValue()));
			assertThat(all.size(), is(1));
			assertThat(all.get(0), is(identityTypeUpdated));
		});
	}
	
	protected IdentityType getIdentityType(String name)
	{
		IdentityType idType = new IdentityType(name);
		idType.setIdentityTypeProvider("identityTypeProvider");
		idType.setIdentityTypeProviderSettings("{}");
		idType.setDescription("d");
		idType.setMaxInstances(10);
		idType.setMinInstances(0);
		idType.setSelfModificable(true);
		idType.getExtractedAttributes().put("a", "b");
		idType.getExtractedAttributes().put("aa", "bb");
		return idType;
	}
}
