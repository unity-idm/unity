/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store;

import java.util.List;
import java.util.Map;

import pl.edu.icm.unity.base.internal.TransactionalRunner;
import pl.edu.icm.unity.store.api.BasicCRUDDAO;
import pl.edu.icm.unity.store.generic.DependencyNotificationManager;

/**
 * Implementation of CRUD DAO using underlying RDBMS and Hazelcast DAOs. Hazelcast is used for reading,
 * both are used for writing.
 *  
 * @author K. Benedyczak
 */
public abstract class GenericCompositeDAOImpl<T> implements BasicCRUDDAO<T>
{
	private BasicCRUDDAO<T> hzDAO;
	private BasicCRUDDAO<T> rdbmsDAO;
	private TransactionalRunner tx;
	private DependencyNotificationManager notificationsManager;
	private String notificationId;

	public GenericCompositeDAOImpl(BasicCRUDDAO<T> hzDAO, BasicCRUDDAO<T> rdbmsDAO,
			TransactionalRunner tx, DependencyNotificationManager notificationsManager,
			String notificationId)
	{
		this.hzDAO = hzDAO;
		this.rdbmsDAO = rdbmsDAO;
		this.tx = tx;
		this.notificationsManager = notificationsManager;
		this.notificationId = notificationId;
	}

	protected abstract String getKey(T obj);

	public void initHazelcast()
	{
		tx.runInTransaction(() -> {
			List<T> all = rdbmsDAO.getAll();
			for (T element: all)
				hzDAO.create(element);
		}); 
	}
	
	@Override
	public void create(T obj)
	{
		notificationsManager.firePreAddEvent(notificationId, obj);
		rdbmsDAO.create(obj);
		hzDAO.create(obj);
	}

	@Override
	public void update(T obj)
	{
		String key = getKey(obj);
		T old = get(key);
		notificationsManager.firePreUpdateEvent(notificationId, old, obj);
		rdbmsDAO.update(obj);
		hzDAO.update(obj);
	}

	@Override
	public void delete(String id)
	{
		T removed = get(id);
		notificationsManager.firePreRemoveEvent(notificationId, removed);
		rdbmsDAO.delete(id);
		hzDAO.delete(id);
	}

	@Override
	public T get(String id)
	{
		return hzDAO.get(id);
	}

	@Override
	public Map<String, T> getAsMap()
	{
		return hzDAO.getAsMap();
	}
	
	@Override
	public List<T> getAll()
	{
		return hzDAO.getAll();
	}
	
	@Override
	public boolean exists(String id)
	{
		return hzDAO.exists(id);
	}
}
