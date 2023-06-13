/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.db.DBDumpContentElements;
import pl.edu.icm.unity.engine.api.ServerManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;

/**
 * Tests the import and export. Only a integration test. Import and export of all stored elements is tested 
 * individually in the storage module.
 * 
 * @author K. Benedyczak
 */
public class TestImportExport extends DBIntegrationTestBase
{
	@Autowired
	private InitializerCommon initializer;
	
	@Autowired
	private UnityServerConfiguration configuration;
	
	
	@Test
	public void test() throws Exception
	{
		initializer.initializeCommonAttributeTypes();
		FileUtils.deleteDirectory(new File(
				configuration.getFileValue(UnityServerConfiguration.WORKSPACE_DIRECTORY, true), 
				ServerManagement.DB_DUMP_DIRECTORY));
		
		int atsSize = aTypeMan.getAttributeTypes().size();
		int idTypesSize = idTypeMan.getIdentityTypes().size();
		
		File exported = serverMan.exportDb(new DBDumpContentElements());
		assertTrue(exported.exists());
		serverMan.importDb(exported);
		
		assertEquals(atsSize, aTypeMan.getAttributeTypes().size());
		assertEquals(idTypesSize, idTypeMan.getIdentityTypes().size());
	}
}
