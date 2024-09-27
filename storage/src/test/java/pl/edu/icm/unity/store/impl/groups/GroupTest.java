/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.groups;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeStatement;
import pl.edu.icm.unity.base.attribute.AttributeStatement.ConflictResolution;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupProperty;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.json.dump.DBDumpContentElements;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.api.BasicCRUDDAO;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.NamedCRUDDAO;
import pl.edu.icm.unity.store.impl.AbstractNamedDAOTest;

public class GroupTest extends AbstractNamedDAOTest<Group>
{
	@Autowired
	private GroupDAO dao;
	
	@Autowired
	private AttributeTypeDAO atDao;

	@BeforeEach
	public void createReferenced()
	{
		tx.runInTransaction(() -> {
			atDao.create(new AttributeType("dynAt", "syntax"));
			atDao.create(new AttributeType("at", "syntax"));
			atDao.create(new AttributeType("dynAt2", "syntax"));
			atDao.create(new AttributeType("at2", "syntax"));
		});
	}
	
	@Test
	public void modificationOfReturnedGroupDoNotAffectItsSubsequentRead()
	{
		tx.runInTransaction(() -> {
			Group original = new Group("/A");
			original.setDisplayedName(new I18nString("DN1"));
			long key = dao.create(original);

			Group returned1 = dao.getByKey(key);
			returned1.setDisplayedName(new I18nString("changed"));

			Group returned2 = dao.getByKey(key);

			assertThat(returned2.getDisplayedName()).isEqualTo(new I18nString("DN1"));
		});
	}
	
	@Test
	public void childGroupsAreRemovedOnParentRemoval()
	{
		tx.runInTransaction(() -> {
			long key = dao.create(new Group("/A"));
			dao.create(new Group("/A/B"));
			dao.create(new Group("/A/B/C"));
			dao.create(new Group("/A/D"));

			dao.deleteByKey(key);

			assertThat(dao.exists("/A")).isFalse();
			assertThat(dao.exists("/A/B")).isFalse();
			assertThat(dao.exists("/A/B/C")).isFalse();
			assertThat(dao.exists("/A/D")).isFalse();
		});
	}
	
	@Test
	public void childGroupNamesAreUpdatedOnParentRename()
	{
		tx.runInTransaction(() -> {
			long key = dao.create(new Group("/A"));
			dao.create(new Group("/A/B"));
			dao.create(new Group("/A/B/C"));
			dao.create(new Group("/A/D"));
			dao.create(new Group("/AAA"));

			dao.updateByKey(key, new Group("/S"));

			assertThat(dao.exists("/A")).isFalse();
			assertThat(dao.exists("/A/B")).isFalse();
			assertThat(dao.exists("/A/B/C")).isFalse();
			assertThat(dao.exists("/A/D")).isFalse();
			
			assertThat(dao.exists("/S")).isTrue();
			assertThat(dao.exists("/S/B")).isTrue();
			assertThat(dao.exists("/S/B/C")).isTrue();
			assertThat(dao.exists("/S/D")).isTrue();
			assertThat(dao.exists("/AAA")).isTrue();
		});
	}
	
	@Test
	public void childGroupNamesAreUpdatedByNameOnParentRename()
	{
		tx.runInTransaction(() -> {
			dao.create(new Group("/A"));
			dao.create(new Group("/A/B"));
			dao.create(new Group("/A/B/C"));
			dao.create(new Group("/A/D"));
			dao.create(new Group("/AAA"));

			dao.updateByName("/A", new Group("/S"));

			assertThat(dao.exists("/A")).isFalse();
			assertThat(dao.exists("/A/B")).isFalse();
			assertThat(dao.exists("/A/B/C")).isFalse();
			assertThat(dao.exists("/A/D")).isFalse();
			
			assertThat(dao.exists("/S")).isTrue();
			assertThat(dao.exists("/S/B")).isTrue();
			assertThat(dao.exists("/S/B/C")).isTrue();
			assertThat(dao.exists("/S/D")).isTrue();
			assertThat(dao.exists("/AAA")).isTrue();
		});
	}
	
	@Test
	public void changingNotTheLastNamePartIsForbidden()
	{
		tx.runInTransaction(() -> {
			dao.create(new Group("/A"));
			dao.create(new Group("/A/B"));
			long key = dao.create(new Group("/A/B/C"));

			Throwable error = catchThrowable(() -> dao.updateByKey(key, new Group("/S/B/C")));

			assertThat(error).isInstanceOf(IllegalArgumentException.class);
		});
	}
	
	/**
	 * Overridden as we always have the '/' extra added.
	 */
	@Test
	@Override
	public void importExportIsIdempotent()
	{
		Group obj = getObject("name1");
		ByteArrayOutputStream os = tx.runInTransactionRet(() -> {
			dao.create(obj);
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try
			{
				ie.store(baos, new DBDumpContentElements());
			} catch (Exception e)
			{
				e.printStackTrace();
				fail("Export failed " + e);
			}
			return baos;
		});
		
		tx.runInTransaction(() -> {
			dbCleaner.cleanOrDelete();
		});

		tx.runInTransaction(() -> {
			String dump = new String(os.toByteArray(), StandardCharsets.UTF_8);
			ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
			try
			{
				ie.load(is);
			} catch (Exception e)
			{
				e.printStackTrace();
				
				fail("Import failed " + e + "\nDump:\n" + dump);
			}

			List<Group> all = dao.getAll();

			assertThat(all).hasSize(2);
			Group g = all.get(0);
			if (g.getName().equals("/"))
				g = all.get(1);
			assertThat(g).isEqualTo(obj);
		});

	}

	@Test
	public void insertedListIsReturned()
	{
		tx.runInTransaction(() -> {
			Group obj1 = getObject("name1");
			Group obj2 = getObject("name2");
			BasicCRUDDAO<Group> dao = getDAO();
			dao.createList(Lists.newArrayList(obj1, obj2));

			List<Group> ret = dao.getAll();

			assertThat(ret).isNotNull();
			assertThat(ret).hasSize(3); // + '/'
			assertThat(ret).contains(obj1, obj2);
		});
	}
	
	@Override
	protected NamedCRUDDAO<Group> getDAO()
	{
		return dao;
	}

	@Override
	protected Group getObject(String name)
	{
		Group ret = new Group("/" + name);
		ret.setDescription(new I18nString("desc"));
		ret.setDisplayedName(new I18nString("dname"));
		ret.setAttributesClasses(Sets.newHashSet("ac1", "ac2"));
		
		Attribute fixedAt = new Attribute("at", "syntax", 
				"/A/" + name, Lists.newArrayList("v1"));
		ret.setAttributeStatements(new AttributeStatement[] {
			new AttributeStatement("cnd1", "/A", ConflictResolution.overwrite, 
					fixedAt),
			new AttributeStatement("cnd2", "/A", ConflictResolution.skip, 
					"dynAt", "dynAExpr")
		});
		return ret;
	}

	@Override
	protected Group mutateObject(Group ret)
	{
		ret.setDescription(new I18nString("desc2"));
		ret.setDisplayedName(new I18nString("dname2"));
		ret.setAttributesClasses(Sets.newHashSet("ac1"));
		
		Attribute fixedAt = new Attribute("at2", "syntax", 
				 ret.getName(), Lists.newArrayList("v2"));
		ret.setAttributeStatements(new AttributeStatement[] {
			new AttributeStatement("cnd3", "/A", ConflictResolution.merge, 
					fixedAt),
			new AttributeStatement("cnd4", "/A", ConflictResolution.merge, 
					"dynAt2", "dynAExpr2")
		});
		return ret;
	}
	
	@Test
	public void shouldSaveGroupProperties()
	{
		tx.runInTransaction(() -> {
			Group g = new Group("/A");
			g.setProperties(Lists.newArrayList(new GroupProperty("k1", "v1"), new GroupProperty("k2", "v2")));
			dao.create(g);
			assertThat(dao.exists("/A")).isTrue();
			Map<String, GroupProperty> properties = dao.get("/A").getProperties();
			assertThat(properties).hasSize(2);
			assertThat(properties.get("k1").value).isEqualTo("v1");
			assertThat(properties.get("k2").value).isEqualTo("v2");
		});
	}
}
