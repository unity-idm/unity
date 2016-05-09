/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api;

/**
 * Entitites DAO
 * @author K. Benedyczak
 */
public interface EntityDAO extends BasicCRUDDAO<StoredEntity>
{
	String DAO_ID = "EntityDAO";
	String NAME = "entity";
	
	void createWithId(StoredEntity obj);
}
