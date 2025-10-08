/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.audit;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.AuditEventManagement;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;

public class AuditEventListenerTest extends DBIntegrationTestBase
{
	private static final Duration DEFAULT_WAIT_TIME = Duration.ofSeconds(20);
	@Autowired
	private AuditEventManagement auditManager;

	@Autowired
	private AuditEventListener auditListener;

	@Autowired
	private AttributeTypeManagement attributeTypeMan;

	private AttributeType typeWithEntityName;

	@BeforeEach
	public void setup() throws Exception
	{
		typeWithEntityName = getType();
		auditManager.enableAuditEvents();
	}

	@AfterEach
	public void cleanup()
	{
		auditManager.disableAuditEvents();
	}

	@AfterEach
	public void after() 
	{
		// make sure entityNameAttribute is to null after test
		auditListener.entityNameAttribute = null;
	}

	@Test
	public void shouldInitializeEntityNameAttributeToNullAfterDbReset() 
	{
		//given
		assertNull(auditListener.entityNameAttribute);
	}

	@Test
	public void shouldSetEntityNameAttributeWhenAttributeIsAdded() throws EngineException 
	{
		//given
		assertNull(auditListener.entityNameAttribute);

		//when
		attributeTypeMan.addAttributeType(typeWithEntityName);

		//then
		Awaitility.with().pollInSameThread().await().atMost(DEFAULT_WAIT_TIME)
			.until(() -> typeWithEntityName.getName().equalsIgnoreCase(auditListener.entityNameAttribute));
	}

	@Test
	public void shouldUnsetEntityNameAttributeWhenAttributeIsRemoved() throws EngineException 
	{
		//given
		assertNull(auditListener.entityNameAttribute);
		initializeAttributeTypeWithEntityName();

		//when
		attributeTypeMan.removeAttributeType(typeWithEntityName.getName(), false);

		//then
		Awaitility.with().pollInSameThread().await().atMost(DEFAULT_WAIT_TIME)
			.until(() -> (auditListener.entityNameAttribute == null));
	}

	@Test
	public void shouldUnsetEntityNameAttributeWhenMetadataIsRemoved() throws EngineException 
	{
		//given
		assertNull(auditListener.entityNameAttribute);
		initializeAttributeTypeWithEntityName();

		//when
		typeWithEntityName.setMetadata(Collections.emptyMap());
		attributeTypeMan.updateAttributeType(typeWithEntityName);

		//then
		Awaitility.with().pollInSameThread().await().atMost(DEFAULT_WAIT_TIME)
			.until(() -> (auditListener.entityNameAttribute == null));
	}

	@Test
	public void shouldSetEntityNameAttributeWhenMetadataIsAdded() throws EngineException 
	{
		//given
		assertNull(auditListener.entityNameAttribute);
		AttributeType type = getType();
		type.setMetadata(Collections.emptyMap());
		attributeTypeMan.addAttributeType(type);

		//when
		assertNull(auditListener.entityNameAttribute);
		Map<String, String> meta = new HashMap<>();
		meta.put(EntityNameMetadataProvider.NAME, "");
		type.setMetadata(meta);
		attributeTypeMan.updateAttributeType(type);

		//then
		Awaitility.with().pollInSameThread().await().atMost(DEFAULT_WAIT_TIME)
			.until(() -> type.getName().equalsIgnoreCase(auditListener.entityNameAttribute));
	}

	@Test
	@Disabled("flaky - race condition?")
	public void shouldSetEntityNameAttributeWhenMetadataRemovedAndAddedToOtherAttribute() throws EngineException 
	{
		//given
		assertNull(auditListener.entityNameAttribute);
		initializeAttributeTypeWithEntityName();
		AttributeType type = getType();
		type.setName("newName");
		type.setMetadata(Collections.emptyMap());
		attributeTypeMan.addAttributeType(type);
		assertEquals(auditListener.entityNameAttribute, typeWithEntityName.getName());

		//when
		typeWithEntityName.setMetadata(Collections.emptyMap());
		attributeTypeMan.updateAttributeType(typeWithEntityName);
		Map<String, String> meta = new HashMap<>();
		meta.put(EntityNameMetadataProvider.NAME, "");
		type.setMetadata(meta);
		attributeTypeMan.updateAttributeType(type);

		//then
		Awaitility.with().pollInSameThread().await().atMost(DEFAULT_WAIT_TIME)
			.until(() -> type.getName().equalsIgnoreCase(auditListener.entityNameAttribute));
	}

	private void initializeAttributeTypeWithEntityName() throws EngineException 
	{
		attributeTypeMan.addAttributeType(typeWithEntityName);
		Awaitility.with().pollInSameThread().await().atMost(DEFAULT_WAIT_TIME)
			.until(() -> typeWithEntityName.getName().equalsIgnoreCase(auditListener.entityNameAttribute));
	}

	private AttributeType getType() 
	{
		AttributeType type = new AttributeType("theName", "string");
		type.setDescription(new I18nString("desc"));
		type.setDisplayedName(new I18nString("Displayed name"));
		type.setUniqueValues(true);
		type.setMaxElements(1);
		type.setMinElements(1);
		type.setSelfModificable(true);
		Map<String, String> meta = new HashMap<>();
		meta.put(EntityNameMetadataProvider.NAME, "");
		type.setMetadata(meta);
		return type;
	}
}