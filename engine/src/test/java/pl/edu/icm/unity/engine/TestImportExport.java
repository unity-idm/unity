/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;


import static org.junit.Assert.*;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.InitDB;
import pl.edu.icm.unity.server.api.ServerManagement;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.utils.DemoContentInitializer;

/**
 * Tests the import and export.
 * TODO:
 * we should have the following test here:
 * 1) all possible kinds of objects are created.
 * 2) export is performed
 * 3) import is performed.
 * 
 * Additionally the exported file should be stored in test resources. After each change of in-DB format
 * there must be another test and another file in the test resources, so import is tested for 
 * all previous formats and the current one. 
 *  
 * @author K. Benedyczak
 */
public class TestImportExport extends DBIntegrationTestBase
{
	@Autowired
	private DemoContentInitializer initializer;
	
	@Autowired
	private InitDB initDb;
	@Autowired
	private DBSessionManager db;
	@Autowired
	private UnityServerConfiguration configuration;
	
	
	@Test
	public void test() throws Exception
	{
		initializer.run();
		FileUtils.deleteDirectory(new File(
				configuration.getFileValue(UnityServerConfiguration.WORKSPACE_DIRECTORY, true), 
				ServerManagement.DB_DUMP_DIRECTORY));
		
		int atsSize = attrsMan.getAttributeTypes().size();
		int idTypesSize = idsMan.getIdentityTypes().size();
		
		File exported = serverMan.exportDb();
		assertTrue(exported.exists());
		serverMan.importDb(exported, false);
		
		assertEquals(atsSize, attrsMan.getAttributeTypes().size());
		assertEquals(idTypesSize, idsMan.getIdentityTypes().size());
		
		serverMan.importDb(exported, true);
		assertEquals(atsSize, attrsMan.getAttributeTypes().size());
		assertEquals(idTypesSize, idsMan.getIdentityTypes().size());
	}
}
