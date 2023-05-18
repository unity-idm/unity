/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.api;

import com.vaadin.flow.component.Component;

import java.util.List;

public interface StickyEnquiryService
{
	Component createStickyEnquiry(List<String> forms);
	boolean isFormsAreApplicable(List<String> forms);
}
