/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.hz;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import pl.edu.icm.unity.store.StorageCleanerImpl;
import pl.edu.icm.unity.store.StorageConfiguration;
import pl.edu.icm.unity.store.StorageEngine;
import pl.edu.icm.unity.store.api.IdentityTypeDAO;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
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
	}

	@Test
	public void shouldRestoreStoredObjects()
	{
		IdentityType idType = getIdentityType("idType");
		tx.runInTransaction(() -> {
			idTypeDAO.create(idType);
		});

		tx.runInTransaction(() -> {
			hzLoader.reloadHzFromRDBMS();
		});
		
		tx.runInTransaction(() -> {
			List<IdentityType> all = idTypeDAO.getAll();
			assertThat(all, is(notNullValue()));
			assertThat(all.size(), is(1));
			assertThat(all.get(0), is(idType));
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
