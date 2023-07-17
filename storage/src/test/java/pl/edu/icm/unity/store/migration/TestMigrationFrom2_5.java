/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import pl.edu.icm.unity.base.endpoint.Endpoint;
import pl.edu.icm.unity.base.json.dump.DBDumpContentElements;
import pl.edu.icm.unity.store.StorageCleanerImpl;
import pl.edu.icm.unity.store.api.ImportExport;
import pl.edu.icm.unity.store.api.generic.EndpointDB;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;

@ExtendWith(SpringExtension.class)
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
	
	
	@BeforeEach
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
		assertThat(endpointDAO.getAll()).hasSize(10);
		Endpoint endpoint = endpointDAO.get("UNITY user's account");
		assertThat(endpoint.getName()).isEqualTo("UNITY user's account");	
		assertThat(endpoint.getConfiguration().getAuthenticationOptions()).hasSize(3);	
		assertThat(endpoint.getConfiguration().getAuthenticationOptions().get(0)).isEqualTo("pwdWeb");
		assertThat(endpoint.getConfiguration().getAuthenticationOptions().get(1)).isEqualTo("oauthWeb");
		assertThat(endpoint.getConfiguration().getAuthenticationOptions().get(2)).isEqualTo("samlWeb");
	}
}
