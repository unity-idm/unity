/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.migration;

import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import pl.edu.icm.unity.store.StorageCleanerImpl;
import pl.edu.icm.unity.store.api.ImportExport;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath*:META-INF/components.xml"})
public class TestMigrationFrom2_8
{
	@Autowired
	protected StorageCleanerImpl dbCleaner;

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
	public void testImportFrom2_8_0()
	{
		tx.runInTransaction(() -> {
			try
			{
				ie.load(new BufferedInputStream(new FileInputStream(
						"src/test/resources/updateData/from2.8.x/"
						+ "testbed-from2.8.0.json")));
				ie.store(new FileOutputStream("target/afterImport.json"));
			} catch (Exception e)
			{
				e.printStackTrace();
				fail("Import failed " + e);
			}
		});
	}
}
