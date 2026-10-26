/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attribute;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.entity.EntityInformation;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.store.StorageCleanerImpl;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.api.AttributesCacheDAO;
import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.store.types.StoredAttribute;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath*:META-INF/components.xml" })
public class AttributesCacheSearchTest
{
	@Autowired
	private AttributesCacheDAO dao;
	@Autowired
	private GroupDAO groupDao;
	@Autowired
	private EntityDAO entDao;
	@Autowired
	private AttributeTypeDAO atDao;
	@Autowired
	private StorageCleanerImpl dbCleaner;
	@Autowired
	private TransactionalRunner tx;

	private long entity;

	@BeforeEach
	public void cleanDB()
	{
		dbCleaner.cleanOrDelete();
		tx.runInTransaction(() ->
		{
			entity = entDao.create(new EntityInformation());
			groupDao.create(new Group("/A"));
			groupDao.create(new Group("/B"));
			atDao.create(new AttributeType("attr", "syntax"));
			atDao.create(new AttributeType("department", "syntax"));
			atDao.create(new AttributeType("email", "syntax"));
		});
	}

	@AfterEach
	public void shutdown()
	{
		dbCleaner.shutdown();
	}

	@Test
	public void shouldStoreAndFindLongMultiValuedAttribute()
	{
		String longValue = "x".repeat(10240);
		List<String> values = List.of(longValue, longValue, "prefix-Needle-suffix");

		tx.runInTransaction(() ->
		{
			dao.createList(List.of(new StoredAttribute(
					new AttributeExt(new Attribute("attr", "syntax", "/A", values), true), entity)));

			assertThat(dao.getEntityAttributes(entity, "/A")).hasSize(1);
			assertThat(dao.findEntitiesWithValueContaining("/A", "needle")).containsExactly(entity);
			assertThat(dao.findEntitiesWithValueContaining("/A", "missing")).isEmpty();
		});
	}

	@Test
	public void shouldMatchOnlyValuesOfGivenAttributeWhenScoped()
	{
		tx.runInTransaction(() ->
		{
			dao.createList(List.of(
					cached("department", "sales"),
					cached("email", "alice@example.com")));

			assertThat(dao.findEntitiesWithAttributeValueContaining("/A", "department", "alice")).isEmpty();
			assertThat(dao.findEntitiesWithAttributeValueContaining("/A", "email", "alice")).containsExactly(entity);
			assertThat(dao.findEntitiesWithValueContaining("/A", "alice")).containsExactly(entity);
		});
	}

	private StoredAttribute cached(String attribute, String value)
	{
		return new StoredAttribute(new AttributeExt(new Attribute(attribute, "syntax", "/A", List.of(value)), true),
				entity);
	}

	@ParameterizedTest
	@ValueSource(ints = { 1999, 2000, 2001, 10240 })
	public void shouldRoundTripAndFindEndOfValueOfBoundaryLength(int length)
	{
		String value = "x".repeat(length - "END".length()) + "END";

		tx.runInTransaction(() ->
		{
			dao.createList(List.of(cached("department", value)));

			assertThat(dao.getEntityAttributes(entity, "/A").get(0).getAttribute().getValues())
					.containsExactly(value);
			assertThat(dao.findEntitiesWithValueContaining("/A", "xend")).containsExactly(entity);
		});
	}

	@Test
	public void shouldRoundTripAndFindJoinedMultivaluesOfMaximumLength()
	{
		List<String> values = List.of("a".repeat(10240), "b".repeat(10240), "c".repeat(10237) + "END");

		tx.runInTransaction(() ->
		{
			dao.createList(List.of(new StoredAttribute(
					new AttributeExt(new Attribute("department", "syntax", "/A", values), true), entity)));

			assertThat(dao.getEntityAttributes(entity, "/A").get(0).getAttribute().getValues())
					.isEqualTo(values);
			assertThat(dao.findEntitiesWithValueContaining("/A", "cend")).containsExactly(entity);
		});
	}

	@ParameterizedTest
	@ValueSource(strings = { "100%", "a_b", "wow!", "50!%", "back\\slash", "it's", "say \"hi\"" })
	public void shouldMatchSpecialCharactersLiterally(String special)
	{
		long other = tx.runInTransactionRet(() -> entDao.create(new EntityInformation()));
		tx.runInTransaction(() ->
		{
			dao.createList(List.of(cached("department", "x" + special + "y"),
					new StoredAttribute(new AttributeExt(new Attribute("department", "syntax", "/A",
							List.of(wildcardLookalike(special))), true), other)));

			assertThat(dao.findEntitiesWithValueContaining("/A", special)).containsExactly(entity);
		});
	}

	@Test
	public void shouldIgnoreCaseOfBothValueAndTerm()
	{
		tx.runInTransaction(() ->
		{
			dao.createList(List.of(cached("department", "Mixed CASE Value")));

			assertThat(dao.findEntitiesWithValueContaining("/A", "mixed case")).containsExactly(entity);
			assertThat(dao.findEntitiesWithValueContaining("/A", "VALUE")).containsExactly(entity);
		});
	}

	@Test
	public void shouldSearchOnlyInGivenGroup()
	{
		tx.runInTransaction(() ->
		{
			dao.createList(List.of(new StoredAttribute(new AttributeExt(
					new Attribute("department", "syntax", "/B", List.of("needle")), true), entity)));

			assertThat(dao.findEntitiesWithValueContaining("/A", "needle")).isEmpty();
			assertThat(dao.findEntitiesWithValueContaining("/B", "needle")).containsExactly(entity);
		});
	}

	@Test
	public void shouldReturnEntityOnceWhenMultipleAttributesMatch()
	{
		tx.runInTransaction(() ->
		{
			dao.createList(List.of(cached("department", "needle"), cached("email", "needle@x")));

			assertThat(dao.findEntitiesWithValueContaining("/A", "needle")).containsExactly(entity);
		});
	}

	/**
	 * A value which would match the term if its LIKE special characters weren't escaped.
	 */
	private static String wildcardLookalike(String special)
	{
		String lookalike = special.replace("!", "").replace("%", "ZZZ").replace("_", "Z");
		return lookalike.equals(special) ? "unrelated" : "x" + lookalike + "y";
	}
}
