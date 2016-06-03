/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.export;

import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.FileInputStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import pl.edu.icm.unity.base.internal.TransactionalRunner;
import pl.edu.icm.unity.store.StorageCleaner;
import pl.edu.icm.unity.store.api.ImportExport;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath*:META-INF/components.xml"})
public class TestImport
{
	@Autowired
	protected StorageCleaner dbCleaner;

	@Autowired
	protected TransactionalRunner tx;
	
	@Autowired
	protected ImportExport ie;
	
	@Before
	public void cleanDB()
	{
		dbCleaner.reset();
	}

	@Test
	public void testImportFrom1_9_x()
	{
		tx.runInTransaction(() -> {
			try
			{
				ie.load(new BufferedInputStream(new FileInputStream(
						"src/test/resources/updateData/from1.9.x/"
						+ "testbed-from1.9.2-complete.json")));
			} catch (Exception e)
			{
				e.printStackTrace();
				fail("Import failed " + e);
			}
			
			//TODO verify all
			
		});
	}
}
