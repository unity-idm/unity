/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.home.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.Route;
import io.imunity.home.HomeEndpointProperties;
import io.imunity.vaadin.elements.Breadcrumb;
import io.imunity.vaadin.endpoint.common.api.StickyEnquiryService;

import javax.annotation.security.PermitAll;
import java.util.List;

@PermitAll
@Breadcrumb(key = "UserHomeUI.accountUpdate")
@Route(value = "/account-update", layout = HomeUiMenu.class)
public class AccountUpdateView extends HomeViewComponent
{

	private final StickyEnquiryService stickyEnquiryService;
	AccountUpdateView(StickyEnquiryService stickyEnquiryService)
	{
		this.stickyEnquiryService = stickyEnquiryService;
	}

	@Override
	public void afterNavigation(AfterNavigationEvent event)
	{
		HomeEndpointProperties properties = ComponentUtil.getData(UI.getCurrent(), HomeEndpointProperties.class);
		List<String> enabledEnquiries = properties.getEnabledEnquiries();
		if (stickyEnquiryService.anyFormApplicableToCurrentUser(enabledEnquiries))
		{
			Component stickyEnquiry = stickyEnquiryService.createApplicableToCurrentUserStickyEnquiryComponent(enabledEnquiries);
			((HasStyle)stickyEnquiry).getStyle().set("height", "20em");
			getContent().removeAll();
			getContent().add(stickyEnquiry);
		}
	}
}
