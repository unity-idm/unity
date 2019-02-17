/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.store.api.generic;

import pl.edu.icm.unity.store.types.StoredCertificate;

/**
 * Easy access to {@link StoredCertificate} storage.
 * 
 * @author P.Piernik
 *
 */
public interface CertificateDB extends NamedCRUDDAOWithTS<StoredCertificate>
{
}
