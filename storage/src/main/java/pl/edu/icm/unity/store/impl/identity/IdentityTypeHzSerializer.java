/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.identity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.store.hz.AbstractSerializer;
import pl.edu.icm.unity.types.basic.IdentityType;

/**
 * Serialization of {@link IdentityType} for hazelcast
 * @author K. Benedyczak
 */
@Component
public class IdentityTypeHzSerializer extends AbstractSerializer<IdentityType>
{
	@Autowired
	public IdentityTypeHzSerializer(IdentityTypeJsonSerializer serializer)
	{
		super(1, IdentityType.class, serializer);
	}
}
