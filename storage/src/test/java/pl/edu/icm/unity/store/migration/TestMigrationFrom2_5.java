/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.migration;

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

import pl.edu.icm.unity.base.db.DBDumpContentElements;
import pl.edu.icm.unity.base.endpoint.Endpoint;
import pl.edu.icm.unity.store.StorageCleanerImpl;
import pl.edu.icm.unity.store.api.ImportExport;
import pl.edu.icm.unity.store.api.generic.EndpointDB;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath*:META-INF/components.xml"})
public class TestMigrationFrom2_5
{
	@Autowired
	protected StorageCleanerImpl dbCleaner;

	@Autowired
	protected TransactionalRunner tx;
	
	@Autowired
	protected ImportExport ie;
	
	@Autowired
	private EndpointDB endpointDAO;
	
	
	@Before
	public void cleanDB()
	{
		dbCleaner.cleanOrDelete();
	}
	
	@Test
	public void testImportFrom2_5_0()
	{
		tx.runInTransaction(() -> {
			try
			{
				ie.load(new BufferedInputStream(new FileInputStream(
						"src/test/resources/updateData/from2.5.x/"
								+ "testbed-from2.5.0.json")));
				ie.store(new FileOutputStream("target/afterImport.json"),  new DBDumpContentElements());
			} catch (Exception e)
			{
				e.printStackTrace();
				fail("Import failed " + e);
			}

			checkEndpointConfiguration();
		});
	}
	
	private void checkEndpointConfiguration()
	{
		assertThat(endpointDAO.getAll().size(), is(10));
		Endpoint endpoint = endpointDAO.get("UNITY user's account");
		assertThat(endpoint.getName(), is("UNITY user's account"));	
		assertThat(endpoint.getConfiguration().getAuthenticationOptions().size(), is(3));	
		assertThat(endpoint.getConfiguration().getAuthenticationOptions().get(0), is("pwdWeb"));
		assertThat(endpoint.getConfiguration().getAuthenticationOptions().get(1), is("oauthWeb"));
		assertThat(endpoint.getConfiguration().getAuthenticationOptions().get(2), is("samlWeb"));
	}
}
