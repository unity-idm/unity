/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.enquiry;

import com.vaadin.flow.component.Component;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.api.StickyEnquiryService;
import org.springframework.stereotype.Service;
import pl.edu.icm.unity.MessageSource;

import java.util.List;

@Service
class StickyEnquiryServiceImpl implements StickyEnquiryService
{
	private final MessageSource msg;
	private final EnquiryResponseEditorController controller;
	private final NotificationPresenter notificationPresenter;

	StickyEnquiryServiceImpl(MessageSource msg, EnquiryResponseEditorController controller, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.controller = controller;
		this.notificationPresenter = notificationPresenter;
	}

	@Override
	public Component createStickyEnquiry(List<String> forms)
	{
		return new StickyEnquiryUpdatableComponent(msg, controller, notificationPresenter, forms);
	}

	@Override
	public boolean isFormsAreApplicable(List<String> forms)
	{
		for (String enquiryForm : forms)
		{
			if (controller.isStickyFormApplicable(enquiryForm))
			{
				return true;
			}
		}
		return false;
	}
}
