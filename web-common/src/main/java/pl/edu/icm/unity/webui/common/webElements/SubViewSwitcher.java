/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.webui.common.webElements;

/**
 * Responsible for switching root component of view
 * @author P.Piernik
 *
 */
public interface SubViewSwitcher
{
	void exitSubView();
	void goToSubView(UnitySubView subview);
	void exitSubViewAndShowUpdateInfo();
}
