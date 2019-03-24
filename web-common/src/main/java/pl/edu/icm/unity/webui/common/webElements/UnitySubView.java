/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.webui.common.webElements;

import java.util.List;

import com.vaadin.ui.Component;

/**
 * 
 * @author P.Piernik
 *
 */
public interface UnitySubView extends Component
{
	List<String> getBredcrumbs();
}
