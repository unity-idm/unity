/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.engine.api.session;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.utils.TypesRegistryBase;

/**
 * Maintains a simple registry of available {@link SessionParticipantType}s.
 * 
 * @author K. Benedyczak
 */
@Component
public class SessionParticipantTypesRegistry extends TypesRegistryBase<SessionParticipantType>
{
	@Autowired(required=false)
	public SessionParticipantTypesRegistry(List<SessionParticipantType> typeElements)
	{
		super(typeElements);
	}

	public SessionParticipantTypesRegistry()
	{
		super(null);
	}

	@Override
	protected String getId(SessionParticipantType from)
	{
		return from.getProtocolType();
	}
}
