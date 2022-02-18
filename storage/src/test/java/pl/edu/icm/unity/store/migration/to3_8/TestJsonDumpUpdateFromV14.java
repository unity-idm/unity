/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.migration.to3_8;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

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
public class TestJsonDumpUpdateFromV14
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
	public void testImportFrom3_7_2()
	{
		tx.runInTransaction(() ->
		{
			try
			{
				ie.load(new BufferedInputStream(
						new FileInputStream("src/test/resources/updateData/from3.7.x/" + "testbed-from3.7.2.json")));
				ie.store(new FileOutputStream("target/afterImport.json"), new DBDumpContentElements());
			} catch (Exception e)
			{
				fail("Import failed " + e);
			}

			checkOAuthEndpointConfiguration();

		});
	}

	private void checkOAuthEndpointConfiguration()
	{
		Endpoint endpoint = endpointDAO.get("UNITY OAuth2 Authorization Server");
		String configuration = endpoint.getConfiguration().getConfiguration();
		Properties configProp = parse(configuration);
		assertThat(configProp.getProperty("unity.oauth2.as.refreshTokenIssuePolicy"), is("ALWAYS"));
		assertThat(configProp.getProperty("unity.oauth2.as.refreshTokenValidity"), is("10"));

		Endpoint endpoint2 = endpointDAO.get("UNITY OAuth2 Authorization Server2");
		String configuration2 = endpoint2.getConfiguration().getConfiguration();
		Properties configProp2 = parse(configuration2);
		assertThat(configProp2.getProperty("unity.oauth2.as.refreshTokenIssuePolicy"), is("NEVER"));
		assertThat(configProp2.getProperty("unity.oauth2.as.refreshTokenValidity"), nullValue());
		
		Endpoint endpoint3 = endpointDAO.get("UNITY OAuth2 Authorization Server3");
		String configuration3 = endpoint3.getConfiguration().getConfiguration();
		Properties configProp3 = parse(configuration3);
		assertThat(configProp3.getProperty("unity.oauth2.as.refreshTokenIssuePolicy"), is("NEVER"));
		assertThat(configProp3.getProperty("unity.oauth2.as.refreshTokenValidity"), nullValue());
	}

	private static Properties parse(String source)
	{
		Properties raw = new Properties();
		try
		{
			raw.load(new StringReader(source));
		} catch (IOException e)
		{
			throw new InternalException("Invalid configuration of the oauth-rp verificator", e);
		}
		return raw;
	}
}
