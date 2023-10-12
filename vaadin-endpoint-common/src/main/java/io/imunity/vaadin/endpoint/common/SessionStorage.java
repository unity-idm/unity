/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common;

import com.vaadin.flow.component.UI;

import java.util.function.Consumer;

public class SessionStorage
{
	public static void getItem(String key, Consumer<String> consumer)
	{
		UI.getCurrent().getPage()
				.executeJs("return window.sessionStorage.getItem($0);", key)
				.then(String.class, consumer::accept);
	}
}
