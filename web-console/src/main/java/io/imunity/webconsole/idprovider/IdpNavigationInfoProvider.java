/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.idprovider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.webconsole.RootNavigationInfoProvider;
import io.imunity.webconsole.WebConsoleNavigationInfoProvider;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;

/**
 * 
 * @author P.Piernik
 *
 */
@Component
public class IdpNavigationInfoProvider implements WebConsoleNavigationInfoProvider
{
	public static final String ID = "IdpProvider";

	private UnityMessageSource msg;
	private RootNavigationInfoProvider parent;

	@Autowired
	public IdpNavigationInfoProvider(UnityMessageSource msg, RootNavigationInfoProvider parent)
	{
		this.msg = msg;
		this.parent = parent;

	}

	@Override
	public NavigationInfo getNavigationInfo()
	{

		return new NavigationInfo.NavigationInfoBuilder(ID, Type.ViewGroup)
				.withParent(parent.getNavigationInfo())
				.withDisplayNameProvider(
						e -> msg.getMessage("WebConsoleMenu.idpProvider"))
				.withPosition(2).build();
	}

}
