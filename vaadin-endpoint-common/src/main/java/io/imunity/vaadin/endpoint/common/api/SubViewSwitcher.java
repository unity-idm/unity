/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.vaadin.endpoint.common.api;


public interface SubViewSwitcher
{
	void exitSubView();
	void goToSubView(UnitySubView subview);
	void exitSubViewAndShowUpdateInfo();
}
