/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.audit;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Repository;

import pl.edu.icm.unity.store.api.AuditEventDAO;
import pl.edu.icm.unity.types.basic.audit.AuditEvent;

@Repository(AuditEventHzStore.STORE_ID)
public class AuditEventHzStore implements AuditEventDAO
{
	public static final String STORE_ID = DAO_ID + "hz";

	@Override
	public long create(AuditEvent obj)
	{
		return 0;
	}

	@Override
	public void createWithId(long id, AuditEvent obj)
	{

	}

	@Override
	public void updateByKey(long id, AuditEvent obj)
	{

	}

	@Override
	public void deleteByKey(long id)
	{

	}

	@Override
	public void deleteAll()
	{

	}

	@Override
	public AuditEvent getByKey(long id)
	{
		return null;
	}

	@Override
	public List<AuditEvent> getAll()
	{
		return null;
	}

	@Override
	public long getCount()
	{
		return 0;
	}

	@Override
	public Set<String> getAllTags()
	{
		return null;
	}

	@Override
	public List<AuditEvent> getOrderedLogs(Date from, Date until, int limit, String order, int direction)
	{
		return null;
	}

}
