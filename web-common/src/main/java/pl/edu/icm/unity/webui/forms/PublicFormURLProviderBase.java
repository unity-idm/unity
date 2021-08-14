/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.forms;

import pl.edu.icm.unity.webui.wellknownurl.PublicViewProvider;

/**
 * Base for public form view providers.
 * @author P.Piernik
 */
public abstract class PublicFormURLProviderBase implements PublicViewProvider
{
	protected abstract String getFormName(String viewAndParameters);
}
