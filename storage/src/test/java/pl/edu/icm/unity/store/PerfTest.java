/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import pl.edu.icm.unity.base.internal.TransactionalRunner;
import pl.edu.icm.unity.base.utils.StopWatch;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.hz.rdbmsflush.RDBMSEventSink;
import pl.edu.icm.unity.store.hz.tx.HzTransactionalRunner;
import pl.edu.icm.unity.store.impl.attributetype.AttributeTypeHzStore;
import pl.edu.icm.unity.store.impl.attributetype.AttributeTypeRDBMSStore;
import pl.edu.icm.unity.store.mocks.MockAttributeSyntax;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionalRunner;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath*:META-INF/components.xml"})
public class PerfTest 
{
	private final int N = 500;
	
	@Autowired
	private StorageCleaner dbCleaner;

	@Autowired @Qualifier(HzTransactionalRunner.NAME)
	private TransactionalRunner txHz;
	
	@Autowired @Qualifier(SQLTransactionalRunner.NAME)
	private TransactionalRunner txSql;
	
	@Autowired
	private AttributeTypeHzStore dao;

	@Autowired
	private RDBMSEventSink rdbmsSink;

	@Autowired
	private AttributeTypeRDBMSStore rdbmsDao;
	
	@Before
	public void cleanDB()
	{
		dbCleaner.reset();
	}


	@Test
	public void testInMemory()
	{
		test(dao, "Hybrid", txHz);

		StopWatch watch = new StopWatch();
		rdbmsSink.consumePresentAndExit();
		watch.printTotal("RDBMS flush: {0}");
	}

	@Test
	public void testRDBMS()
	{
		test(rdbmsDao, "RDBMS", txSql);
	}

	public void test(AttributeTypeDAO dao, String type, TransactionalRunner tx)
	{
		Random r = new Random();
		
		StopWatch watch = new StopWatch();
		for (int i = 0; i<N; i++)
		{
			int fi = i;
			tx.runInTransaction(() -> {
//				StopWatch watch2 = new StopWatch();
				AttributeType at = getObject("a" + fi); 
				dao.create(at);
				at.setFlags(2);
				at.setDescription(new I18nString("Updated at"));
				dao.update(at);
//				watch2.printTotal(type + " inner create: {0}");
			});
		}
		
		
		watch.printPeriod(type + " creation: {0}");
		for (int i = 0; i<N*5; i++)
		{
			tx.runInTransaction(() -> {
				dao.get("a" + r.nextInt(N));
			});
		}
		watch.printPeriod(type + " read: {0}");

		for (int i = 0; i<N; i++)
		{
			int fi = i;
			tx.runInTransaction(() -> {
				dao.delete("a" + fi);
			});
		}
		watch.printPeriod(type + " delete: {0}");

		
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
