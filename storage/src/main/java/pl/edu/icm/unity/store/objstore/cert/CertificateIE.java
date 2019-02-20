/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.cert;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.store.api.generic.CertificateDB;
import pl.edu.icm.unity.store.objstore.GenericObjectIEBase;
import pl.edu.icm.unity.store.types.StoredCertificate;

/**
 * Handles import/export of {@link Certificate}.
 * @author P. Piernik
 */
@Component
public class CertificateIE extends GenericObjectIEBase<StoredCertificate>
{
	@Autowired
	public CertificateIE(CertificateDB dao, ObjectMapper jsonMapper)
	{
		super(dao, jsonMapper, StoredCertificate.class, 118, CertificateHandler.CERTIFICATE_OBJECT_TYPE);
	}
}



