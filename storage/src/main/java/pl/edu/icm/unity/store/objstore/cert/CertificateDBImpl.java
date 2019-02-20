/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.cert;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.store.api.generic.CertificateDB;
import pl.edu.icm.unity.store.impl.objstore.ObjectStoreDAO;
import pl.edu.icm.unity.store.objstore.GenericObjectsDAOImpl;
import pl.edu.icm.unity.store.types.StoredCertificate;

/**
 * Easy to use interface to {@link Certificate} storage.
 *  
 * @author P. Piernik
 */
@Component
public class CertificateDBImpl extends GenericObjectsDAOImpl<StoredCertificate> implements CertificateDB
{

	@Autowired
	public CertificateDBImpl(CertificateHandler handler, ObjectStoreDAO dbGeneric)
	{
		super(handler, dbGeneric, StoredCertificate.class, "certificate");
	}
}

