/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.signupAndEnquiry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.WebConsoleRootNavigationInfoProvider;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.Images;

/**
 * Provides @{link {@link NavigationInfo} about signup and enquiry submenu
 * 
 * @author P.Piernik
 *
 */
@Component
public class SignupAndEnquiryNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
{
	public static final String ID = "SignupAndEnquiry";

	@Autowired
	public SignupAndEnquiryNavigationInfoProvider(MessageSource msg)
	{
		super(new NavigationInfo.NavigationInfoBuilder(ID, Type.ViewGroup)
				.withParent(WebConsoleRootNavigationInfoProvider.ID)
				.withCaption(msg.getMessage("WebConsoleMenu.signupAndEnquiry"))
				.withIcon(Images.user_check.getResource()).withPosition(30).build());

	}

}
