/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm.token;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.utils.TypesRegistryBase;
import pl.edu.icm.unity.engine.api.utils.json.TokenContentsJsonSerializer;

/**
 * Maintains a simple registry of available {@link TokenContentsJsonSerializer}ies.
 * 
 * @author P. Piernik
 */
@Component
public class JsonFormatterFacilitiesRegistry extends TypesRegistryBase<TokenContentsJsonSerializer>
{
	@Autowired
	public JsonFormatterFacilitiesRegistry(List<TokenContentsJsonSerializer> typeElements)
	{
		super(typeElements);
	}

	@Override
	protected String getId(TokenContentsJsonSerializer from)
	{
		return from.getName();
	}
	
	public TokenContentsJsonSerializer getFormatter(String type)
	{
		TokenContentsJsonSerializer specificFormatter = getByNameOptional(type);
		return specificFormatter != null ? specificFormatter : 
			getByName(DefaultJsonFormatterFacility.NAME);
	}
}
