/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth.authenticators;

import org.springframework.stereotype.Component;
import pl.edu.icm.unity.engine.api.utils.TypesRegistryBase;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Component
public class AuthenticatorEditorFactoriesRegistry extends TypesRegistryBase<AuthenticatorEditorFactory>
{
	public AuthenticatorEditorFactoriesRegistry(Optional<List<AuthenticatorEditorFactory>> typeElements)
	{
		super(typeElements.orElseGet(ArrayList::new));
	}

	@Override
	protected String getId(AuthenticatorEditorFactory from)
	{
		return from.getSupportedAuthenticatorType();
	}
}

