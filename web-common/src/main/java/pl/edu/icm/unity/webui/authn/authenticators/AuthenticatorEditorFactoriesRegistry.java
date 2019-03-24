/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.webui.authn.authenticators;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.utils.TypesRegistryBase;

/**
 * Maintains a simple registry of available {@link AuthenticatorEditorFactory}ies.
 * @author P.Piernik
 *
 */
@Component("AuthenticatorEditorFactoriesRegistry")
public class AuthenticatorEditorFactoriesRegistry extends TypesRegistryBase<AuthenticatorEditorFactory>
{
	@Autowired
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

