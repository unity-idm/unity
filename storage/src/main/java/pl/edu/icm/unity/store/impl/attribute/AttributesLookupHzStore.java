/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attribute;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IAtomicLong;
import com.hazelcast.core.TransactionalMap;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.hz.HzDAO;
import pl.edu.icm.unity.store.hz.rdbmsflush.RDBMSMutationEvent;
import pl.edu.icm.unity.store.hz.tx.HzTransactionTL;

@Repository(AttributesLookupHzStore.STORE_ID)
public class AttributesLookupHzStore implements HzDAO
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_DB, AttributesLookupHzStore.class);
	static final String STORE_ID = "AttributesLookupHz";
	
	private final AttributesLookupRDBMSDAO rdmsDAO;
	private IAtomicLong index;
	
	public AttributesLookupHzStore(AttributesLookupRDBMSDAO rdmsDAO)
	{
		this.rdmsDAO = rdmsDAO;
	}
	
	void linkKeywordToAttribute(String keyword, long attributeId)
	{
		AttributeLookupBean obj = new AttributeLookupBean(null, keyword, attributeId);
		createNoPropagateToRDBMS(obj);
		HzTransactionTL.enqueueRDBMSMutation(new RDBMSMutationEvent(AttributesLookupRDBMSDAO.DAO_ID, 
				"createWithKey", obj));
	}
	
	Set<Long> getAllWithKeyword(String keyword)
	{
		return getMap().values().stream()
			.filter(bean -> bean.getkeyword().equals(keyword))
			.map(AttributeLookupBean::getAttributeId)
			.collect(Collectors.toSet());
	}
	
	List<String> getAllKeywordsFor(Long attributeId)
	{
		return getMap().values().stream()
			.filter(bean -> bean.getAttributeId().equals(attributeId))
			.map(AttributeLookupBean::getkeyword)
			.collect(Collectors.toList());
	}

	@Override
	public void populateFromRDBMS(HazelcastInstance hzInstance)
	{
		LOG.info("Loading AttributeLookupBean from persistent storage");
		index = hzInstance.getAtomicLong(STORE_ID);
		if (getMap().size() != 0)
			throw new IllegalStateException("In-memory data is non empty before loading AttributeLookupBean, "
					+ "have " + getMap().size() + " entries");

		List<AttributeLookupBean> all = rdmsDAO.getAll();
		for (AttributeLookupBean element: all)
			createNoPropagateToRDBMS(element);
		
	}
	
	private long createNoPropagateToRDBMS(AttributeLookupBean obj) throws IllegalArgumentException
	{
		TransactionalMap<Long, AttributeLookupBean> hMap = getMap();
		long key = index.incrementAndGet();
		while (hMap.containsKey(key))
			key = index.incrementAndGet();
		obj.setId(key);
		hMap.put(key, obj);
		return key;
	}
	
	private TransactionalMap<Long, AttributeLookupBean> getMap()
	{
		return HzTransactionTL.getHzContext().getMap(STORE_ID);
	}
}
