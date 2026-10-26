/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;


import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.entity.EntityState;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.json.dump.DBDumpContentElements;
import pl.edu.icm.unity.engine.api.ServerManagement;
import pl.edu.icm.unity.engine.api.attributes.CachedAttributes;
import pl.edu.icm.unity.engine.api.attributes.EffectiveAttributesCacheService;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.X500Identity;

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

	@Autowired
	private EffectiveAttributesCacheService effectiveAttributesCache;
	
	
	@Test
	public void test() throws Exception
	{
		initializer.initializeCommonAttributeTypes();
		deleteDumpsDirectory();
		
		int atsSize = aTypeMan.getAttributeTypes().size();
		int idTypesSize = idTypeMan.getIdentityTypes().size();
		
		File exported = serverMan.exportDb(new DBDumpContentElements());
		assertThat(exported.exists()).isTrue();
		serverMan.importDb(exported);
		
		assertThat(atsSize).isEqualTo(aTypeMan.getAttributeTypes().size());
		assertThat(idTypesSize).isEqualTo(idTypeMan.getIdentityTypes().size());
	}

	@Test
	public void attributesCacheIsRegeneratedAfterImport() throws Exception
	{
		setupMockAuthn();
		aTypeMan.addAttributeType(new AttributeType("a1", StringAttributeSyntax.ID));
		groupsMan.addGroup(new Group("/A"));
		Identity id = idsMan.addEntity(new IdentityParam(X500Identity.ID, "cn=test"), "crMock",
				EntityState.valid);
		EntityParam entity = new EntityParam(id);
		groupsMan.addMemberFromParent("/A", entity);
		attrsMan.createAttribute(entity, StringAttribute.of("a1", "/A", "v1"));
		awaitCachedAttribute(id.getEntityId(), "/A", "a1");
		deleteDumpsDirectory();

		File exported = serverMan.exportDb(new DBDumpContentElements());
		serverMan.importDb(exported);

		awaitCachedAttribute(id.getEntityId(), "/A", "a1");
	}

	private void deleteDumpsDirectory() throws Exception
	{
		FileUtils.deleteDirectory(new File(
				configuration.getFileValue(UnityServerConfiguration.WORKSPACE_DIRECTORY, true), 
				ServerManagement.DB_DUMP_DIRECTORY));
	}

	private void awaitCachedAttribute(long entityId, String group, String attribute)
	{
		Awaitility.await().pollInSameThread().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
		{
			CachedAttributes cached = effectiveAttributesCache.getAttributesFast(entityId, group);
			assertThat(cached.updatePending()).isFalse();
			assertThat(cached.attributes()).containsKey(attribute);
		});
	}
}
