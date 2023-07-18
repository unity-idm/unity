/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;


import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.json.dump.DBDumpContentElements;
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
		assertThat(exported.exists()).isTrue();
		serverMan.importDb(exported);
		
		assertThat(atsSize).isEqualTo(aTypeMan.getAttributeTypes().size());
		assertThat(idTypesSize).isEqualTo(idTypeMan.getIdentityTypes().size());
	}
}
