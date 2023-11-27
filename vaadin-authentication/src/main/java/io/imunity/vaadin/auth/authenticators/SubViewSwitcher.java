/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.vaadin.auth.authenticators;

import pl.edu.icm.unity.webui.common.webElements.UnitySubView;

public interface SubViewSwitcher
{
	void exitSubView();
	void goToSubView(UnitySubView subview);
	void exitSubViewAndShowUpdateInfo();
}
