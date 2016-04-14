/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import pl.edu.icm.unity.base.internal.TransactionalRunner;
import pl.edu.icm.unity.base.utils.StopWatch;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.impl.attribute.AttributeTypeHzStore;
import pl.edu.icm.unity.store.impl.attribute.AttributeTypeRDBMSStore;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath*:META-INF/components.xml"})
public class PerfTest 
{
	private final int N = 1000;
	
	@Autowired
	private StoreLoader dbCleaner;

	@Autowired
	protected TransactionalRunner tx;

	@Autowired
	private AttributeTypeHzStore dao;

	@Autowired
	private AttributeTypeRDBMSStore rdbmsDao;
	
	@Before
	public void cleanDB()
	{
		dbCleaner.resetDatabase();
	}


	@Test
	public void testHybrid()
	{
		test(dao, "Hybrid");
	}

	@Test
	public void testRDBMS()
	{
		test(rdbmsDao, "RDBMS");
	}

	public void test(AttributeTypeDAO dao, String type)
	{
		StopWatch watch = new StopWatch();
		for (int i = 0; i<N; i++)
		{
			int fi = i;
			tx.runInTransaction(() -> {
//				StopWatch watch2 = new StopWatch();
				dao.create(getObject("a" + fi));
//				watch2.printTotal(type + " inner create: {0}");
			});
		}
		
		watch.printPeriod(type + " creation: {0}");
		for (int i = 0; i<N; i++)
		{
			int fi = i;
			tx.runInTransaction(() -> {
//				StopWatch watch2 = new StopWatch();
				dao.get("a" + fi);
//				watch2.printTotal(type + " inner read: {0}");
			});
		}
		watch.printPeriod(type + " read: {0}");
		watch.printTotal(type + " total: {0}");
	}
	
	protected AttributeType getObject(String name)
	{
		AttributeType created = new AttributeType(name, new MockAttributeSyntax());
		created.setDescription(new I18nString("desc"));
		created.setDisplayedName(new I18nString("Attribute 1"));
		created.setFlags(8);
		created.setUniqueValues(true);
		created.setVisibility(AttributeVisibility.local);
		created.setMaxElements(10);
		created.setMinElements(1);
		created.setSelfModificable(true);
		Map<String, String> meta = new HashMap<>();
		meta.put("1", "a");
		created.setMetadata(meta);
		return created;
	}
}
