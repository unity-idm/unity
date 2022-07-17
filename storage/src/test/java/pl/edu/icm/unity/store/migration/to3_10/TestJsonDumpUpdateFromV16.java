/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.migration.to3_10;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.store.StorageCleanerImpl;
import pl.edu.icm.unity.store.api.ImportExport;
import pl.edu.icm.unity.store.api.generic.EndpointDB;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.types.basic.DBDumpContentElements;
import pl.edu.icm.unity.types.endpoint.Endpoint;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations =
{ "classpath*:META-INF/components.xml" })
public class TestJsonDumpUpdateFromV16
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
	public void testImportFrom3_9_1()
	{
		tx.runInTransaction(() ->
		{
			try
			{
				ie.load(new BufferedInputStream(
						new FileInputStream("src/test/resources/updateData/from3.9.x/" + "testbed-from3.9.1.json")));
				ie.store(new FileOutputStream("target/afterImport.json"), new DBDumpContentElements());
			} catch (Exception e)
			{
				fail("Import failed " + e);
			}
			checkUserHomeEndpointConfiguration();

		});
	}
	
	private void checkUserHomeEndpointConfiguration()
	{
		Endpoint endpoint = endpointDAO.get("UNITY user's account");
		String configuration = endpoint.getConfiguration().getConfiguration();
		Properties configProp = parse(configuration);
		assertThat(configProp.getProperty("unity.userhome.disabledComponents.2"), nullValue());
		assertThat(configProp.getProperty("unity.userhome.disabledComponents.8"), nullValue());

		boolean trustedTabDisabled = false;
		for (Object key : configProp.keySet().stream().filter(k -> k.toString().startsWith("unity.userhome.disabledComponents.")).collect(Collectors.toSet()))
		{
			if (configProp.get(key).equals("trustedApplicationTab"))
			{
				trustedTabDisabled = true;
			}
		}
		
		if (!trustedTabDisabled)
		{
			fail("trustedApplicationTab should be disabled");
		}
		
		
	}
	
	private static Properties parse(String source)
	{
		Properties raw = new Properties();
		try
		{
			raw.load(new StringReader(source));
		} catch (IOException e)
		{
			throw new InternalException("Invalid configuration of UserHome endpoint", e);
		}
		return raw;
	}
}
