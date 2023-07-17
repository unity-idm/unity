/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.migration.to3_4;



import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Sets;

import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.json.dump.DBDumpContentElements;
import pl.edu.icm.unity.store.StorageCleanerImpl;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.ImportExport;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath*:META-INF/components.xml" })
public class TestJsonDumpUpdateFromV11
{
	@Autowired
	protected StorageCleanerImpl dbCleaner;

	@Autowired
	protected TransactionalRunner tx;

	@Autowired
	protected ImportExport ie;

	@Autowired
	private AttributeTypeDAO attrTypeDAO;
	
	@Autowired
	private GroupDAO groupDao;

	@BeforeEach
	public void cleanDB()
	{
		dbCleaner.cleanOrDelete();
	}

	@Test
	public void testImportFrom2_8_0()
	{
		tx.runInTransaction(() -> {
			try
			{
				ie.load(new BufferedInputStream(
						new FileInputStream("src/test/resources/updateData/from3.3.x/"
								+ "testbed-from3.3.4.json")));
				ie.store(new FileOutputStream("target/afterImport.json"), new DBDumpContentElements());
			} catch (Exception e)
			{
				e.printStackTrace();
				fail("Import failed " + e);
			}

			checkProjectRoleAttributeTypeConfig();
			checkGroups();
		});

	}

	private void checkProjectRoleAttributeTypeConfig()
	{
		AttributeType type = attrTypeDAO.get("sys:ProjectManagementRole");
		assertThat(type.getValueSyntaxConfiguration().get("allowed").isArray()).isTrue();
		ArrayNode config = (ArrayNode) type.getValueSyntaxConfiguration().get("allowed");
		assertThat(config).hasSize(3);
		assertThat(config.get(0).asText()).isEqualTo("manager");
		assertThat(StreamSupport.stream(config.spliterator(), false).map(a -> a.asText())
				.collect(Collectors.toSet())
				.containsAll(Sets.newHashSet("manager", "projectsAdmin", "regular"))).isTrue();

	}

	
	private void checkGroups()
	{
		assertThat(groupDao.getAll()).hasSize(9);
		assertThat(groupDao.getAll().get(0).getDelegationConfiguration().enableSubprojects).isFalse();
	}
	
	
}
