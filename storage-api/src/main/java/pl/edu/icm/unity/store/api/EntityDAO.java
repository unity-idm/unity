/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api;

import java.util.List;

import pl.edu.icm.unity.types.basic.EntityInformation;

/**
 * Entities DAO
 * @author K. Benedyczak
 */
public interface EntityDAO extends BasicCRUDDAO<EntityInformation>
{
	String DAO_ID = "EntityDAO";
	String NAME = "entity";
	
	List<EntityInformation> getByGroup(String group);
}
