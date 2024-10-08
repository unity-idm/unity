/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.migration.to3_8;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import pl.edu.icm.unity.base.endpoint.Endpoint;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.json.dump.DBDumpContentElements;
import pl.edu.icm.unity.store.StorageCleanerImpl;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.api.ImportExport;
import pl.edu.icm.unity.store.api.generic.EndpointDB;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;

@ExtendWith(SpringExtension.class)
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

	@Autowired
	private AttributeTypeDAO atTypeDAO;

	
	@BeforeEach
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
			checkStringAttributeSyntax();
			checkOAuthEndpointConfiguration();

		});
	}

	private void checkOAuthEndpointConfiguration()
	{
		Endpoint endpoint = endpointDAO.get("UNITY OAuth2 Authorization Server");
		String configuration = endpoint.getConfiguration().getConfiguration();
		Properties configProp = parse(configuration);
		assertThat(configProp.getProperty("unity.oauth2.as.refreshTokenIssuePolicy")).isEqualTo("ALWAYS");
		assertThat(configProp.getProperty("unity.oauth2.as.refreshTokenValidity")).isEqualTo("10");

		Endpoint endpoint2 = endpointDAO.get("UNITY OAuth2 Authorization Server2");
		String configuration2 = endpoint2.getConfiguration().getConfiguration();
		Properties configProp2 = parse(configuration2);
		assertThat(configProp2.getProperty("unity.oauth2.as.refreshTokenIssuePolicy")).isEqualTo("NEVER");
		assertThat(configProp2.getProperty("unity.oauth2.as.refreshTokenValidity")).isNull();
		
		Endpoint endpoint3 = endpointDAO.get("UNITY OAuth2 Authorization Server3");
		String configuration3 = endpoint3.getConfiguration().getConfiguration();
		Properties configProp3 = parse(configuration3);
		assertThat(configProp3.getProperty("unity.oauth2.as.refreshTokenIssuePolicy")).isEqualTo("NEVER");
		assertThat(configProp3.getProperty("unity.oauth2.as.refreshTokenValidity")).isNull();
	}
	
	private void checkStringAttributeSyntax()
	{
		assertThat(atTypeDAO.get("address").getValueSyntaxConfiguration().get("editWithTextArea").asBoolean()).isFalse();
		assertThat(atTypeDAO.get("pgpPublicKey").getValueSyntaxConfiguration().get("editWithTextArea").asBoolean()).isTrue();
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
