/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.settings.pki.cert;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.settings.pki.PKIView.PKINavigationInfoProvider;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import pl.edu.icm.unity.base.message.MessageSource;

/**
 * Provides @{link {@link NavigationInfo} about trusted certificates submenu
 * 
 * @author P.Piernik
 *
 */
@Component
class TrustedCertNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
{
	public static final String ID = "TrustedCertificates";

	@Autowired
	TrustedCertNavigationInfoProvider(MessageSource msg)
	{
		super(new NavigationInfo.NavigationInfoBuilder(ID, Type.ViewGroup)
				.withParent(PKINavigationInfoProvider.ID)
				.withCaption(msg.getMessage("TrustedCertificates.navCaption"))
				.build());

	}

}
