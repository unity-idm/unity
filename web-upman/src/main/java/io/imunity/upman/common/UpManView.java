/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.common;

import com.vaadin.ui.Component;

import io.imunity.webelements.navigation.UnityView;

/**
 * In principle all UpMan View should implement this interface. View header is
 * used as page title component.
 * 
 * @author P.Piernik
 *
 */
public interface UpManView extends UnityView
{
	Component getViewHeader();
}
