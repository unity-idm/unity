/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.identity_provider.endpoints;

import java.util.List;

import org.springframework.stereotype.Component;

import io.imunity.console.spi.IdpServiceAdditionalAction;
import pl.edu.icm.unity.engine.api.utils.TypesRegistryBase;

/**
 * Provides additional actions for idp services view
 * 
 * @author P.Piernik
 *
 */
@Component
public class IdpServiceAdditionalActionsRegistry extends TypesRegistryBase<IdpServiceAdditionalAction>
{
	public IdpServiceAdditionalActionsRegistry(List<? extends IdpServiceAdditionalAction> typeElements)
	{
		super(typeElements);
	}

	@Override
	protected String getId(IdpServiceAdditionalAction from)
	{
		return from.getName();
	}

}
