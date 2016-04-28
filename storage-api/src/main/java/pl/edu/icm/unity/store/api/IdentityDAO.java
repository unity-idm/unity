/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api;

import java.util.List;

import pl.edu.icm.unity.types.basic.Identity;

/**
 * Identity DAO
 * @author K. Benedyczak
 */
public interface IdentityDAO extends NamedCRUDDAO<Identity>
{
	String DAO_ID = "IdentityDAO";
	String NAME = "identity";

	List<Identity> getByEntity(long entityId);
}
