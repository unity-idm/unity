/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.migration.to4_0;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import pl.edu.icm.unity.store.StorageCleanerImpl;
import pl.edu.icm.unity.store.api.ImportExport;
import pl.edu.icm.unity.store.api.generic.RegistrationFormDB;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;

import java.io.BufferedInputStream;
import java.io.FileInputStream;

import static org.junit.Assert.fail;

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
	private RegistrationFormDB registrationFormDB;

	
	@Before
	public void cleanDB()
	{
		dbCleaner.cleanOrDelete();
	}

	@Test
	public void testImportFrom4_0_0()
	{
		tx.runInTransaction(() ->
		{
			try
			{
				ie.load(new BufferedInputStream(
						new FileInputStream("src/test/resources/updateData/from4.0.x/" + "testbed-from4.0.0.json")));
			} catch (Exception e)
			{
				fail("Import failed " + e);
			}
			checkFidoIdentityHasBeenRemoved();
		});
	}
	
	private void checkFidoIdentityHasBeenRemoved()
	{
		registrationFormDB.getAll().stream()
				.flatMap(form -> form.getIdentityParams().stream())
				.filter(identity -> identity.getIdentityType().equals("fidoUserHandle"))
				.findAny().ifPresent(identity -> fail("fidoUserHandle identity should be removed"));
	}

}
