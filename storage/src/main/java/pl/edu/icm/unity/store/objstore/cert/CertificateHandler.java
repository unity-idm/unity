/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.cert;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.store.impl.objstore.GenericObjectBean;
import pl.edu.icm.unity.store.objstore.DefaultEntityHandler;
import pl.edu.icm.unity.store.types.StoredCertificate;

/**
 * Handler for {@link Certificate}s storage.
  * @author P. Piernik
 */
@Component
public class CertificateHandler extends DefaultEntityHandler<StoredCertificate>
{
	public static final String CERTIFICATE_OBJECT_TYPE = "certificate";
	
	@Autowired
	public CertificateHandler(ObjectMapper jsonMapper)
	{
		super(jsonMapper, CERTIFICATE_OBJECT_TYPE, StoredCertificate.class);
	}

	
	@Override
	public GenericObjectBean toBlob(StoredCertificate value)
	{
		try
		{
			byte[] contents = jsonMapper.writeValueAsBytes(value);
			return new GenericObjectBean(value.getName(), contents, supportedType);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize certificate JSON", e);
		}
	}

	@Override
	public StoredCertificate fromBlob(GenericObjectBean blob)
	{
		try
		{
			return jsonMapper.readValue(blob.getContents(), StoredCertificate.class);
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize certificate from JSON", e);
		}
	}
}
