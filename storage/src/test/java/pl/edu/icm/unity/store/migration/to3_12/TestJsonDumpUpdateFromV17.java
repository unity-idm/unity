/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.migration.to3_12;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
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
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.api.ImportExport;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.types.basic.DBDumpContentElements;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations =
{ "classpath*:META-INF/components.xml" })
public class TestJsonDumpUpdateFromV17
{
	@Autowired
	protected StorageCleanerImpl dbCleaner;

	@Autowired
	protected TransactionalRunner tx;

	@Autowired
	protected ImportExport ie;

	@Autowired
	private AttributeTypeDAO atTypeDAO;

	@Before
	public void cleanDB()
	{
		dbCleaner.cleanOrDelete();
	}

	@Test
	public void testImportFrom3_9_1()
	{
		tx.runInTransaction(() ->
		{
			try
			{
				ie.load(new BufferedInputStream(
						new FileInputStream("src/test/resources/updateData/from3.11.2/" + "testbed-from3.11.2.json")));
				ie.store(new FileOutputStream("target/afterImport.json"), new DBDumpContentElements());
			} catch (Exception e)
			{
				fail("Import failed " + e);
			}
			checkRoleAttribute();

		});
	}

	private void checkRoleAttribute()
	{
		assertThat(atTypeDAO.get("sys:AuthorizationRole")
				.getDescription()
				.getValue("en")
				.contains("Policy documents manager"), is(true));
	}
}
