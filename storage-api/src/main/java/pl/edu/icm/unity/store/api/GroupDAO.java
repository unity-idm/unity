/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api;

import java.util.List;

import pl.edu.icm.unity.types.basic.Group;

/**
 * Group DAO
 * @author K. Benedyczak
 */
public interface GroupDAO extends NamedCRUDDAO<Group>
{
	String DAO_ID = "GroupDAO";
	String NAME = "group";
	
	List<Group> getGroupChain(String path);
}
