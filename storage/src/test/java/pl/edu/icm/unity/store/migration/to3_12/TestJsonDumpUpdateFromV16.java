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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.db.DBDumpContentElements;
import pl.edu.icm.unity.base.utils.JsonUtil;
import pl.edu.icm.unity.store.StorageCleanerImpl;
import pl.edu.icm.unity.store.api.ImportExport;
import pl.edu.icm.unity.store.api.TokenDAO;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;

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
	private TokenDAO tokenDAO;

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
				ie.load(new BufferedInputStream(new FileInputStream(
						"src/test/resources/updateData/from3.9.x/" + "testbed-from3.9.1WithOAuthTokens.json")));
				ie.store(new FileOutputStream("target/afterImport.json"), new DBDumpContentElements());
			} catch (Exception e)
			{
				fail("Import failed " + e);
			}
			checkOAuthTokens();

		});
	}

	private void checkOAuthTokens()
	{
		byte[] tokenContents = tokenDAO.get("oauth2Access", "Cwi6RVzcr3gLfP9FK21DLJInG73Lh7bNtNknKl8C6uE")
				.getContents();
		ObjectNode objContent = JsonUtil.parse(tokenContents);
		JsonNode jsonNode = objContent.get("audience");
		assertThat(jsonNode.isArray(), is(true));
		assertThat(jsonNode.get(0).asText(), is("oauth-client"));
	}
}
