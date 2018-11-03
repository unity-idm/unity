/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api;

import java.util.List;

import pl.edu.icm.unity.store.types.StoredIdentity;
import pl.edu.icm.unity.types.basic.Identity;

/**
 * Identity DAO
 * @author K. Benedyczak
 */
public interface IdentityDAO extends NamedCRUDDAO<StoredIdentity>
{
	String DAO_ID = "IdentityDAO";
	String NAME = "identity";

	List<Identity> getByEntity(long entityId);
	List<StoredIdentity> getByEntityFull(long entityId);
	List<StoredIdentity> getByGroup(String group);
}
