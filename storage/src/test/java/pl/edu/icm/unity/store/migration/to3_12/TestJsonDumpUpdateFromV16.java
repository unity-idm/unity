/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.migration.to3_12;

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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.json.JsonUtil;
import pl.edu.icm.unity.base.json.dump.DBDumpContentElements;
import pl.edu.icm.unity.store.StorageCleanerImpl;
import pl.edu.icm.unity.store.api.ImportExport;
import pl.edu.icm.unity.store.api.TokenDAO;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;

@ExtendWith(SpringExtension.class)
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

	@BeforeEach
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
		assertThat(jsonNode.isArray()).isTrue();
		assertThat(jsonNode.get(0).asText()).isEqualTo("oauth-client");
	}
}
