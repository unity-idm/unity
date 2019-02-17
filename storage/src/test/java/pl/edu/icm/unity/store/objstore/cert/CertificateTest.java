/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.cert;

import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.store.api.generic.CertificateDB;
import pl.edu.icm.unity.store.api.generic.NamedCRUDDAOWithTS;
import pl.edu.icm.unity.store.objstore.AbstractNamedWithTSTest;
import pl.edu.icm.unity.store.types.StoredCertificate;

/**
 * 
 * @author P.Piernik
 *
 */
public class CertificateTest extends AbstractNamedWithTSTest<StoredCertificate>
{
	@Autowired
	private CertificateDB certificateDB;

	@Override
	protected NamedCRUDDAOWithTS<StoredCertificate> getDAO()
	{
		return certificateDB;
	}

	@Override
	protected StoredCertificate getObject(String id)
	{
		
		return new StoredCertificate(id, "val");
	}

	@Override
	protected StoredCertificate mutateObject(StoredCertificate src)
	{
		src.setName("name-Changed");
		src.setValue("val2");
		return src;
	}
}
