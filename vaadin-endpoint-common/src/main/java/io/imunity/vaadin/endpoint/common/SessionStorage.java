/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common;

import java.net.URL;

import com.vaadin.flow.component.UI;

public class SessionStorage
{
	public static final String REDIRECT_URL_SESSION_STORAGE_KEY = "redirect-url";
	
	public static void consumeRedirectUrl(StoredValueConsumer consumer)
	{
		consumeSessionStorageItem(REDIRECT_URL_SESSION_STORAGE_KEY, consumer);
	}
	
	private static void consumeSessionStorageItem(String key, StoredValueConsumer consumer)
	{
		UI.getCurrent().getPage().fetchCurrentURL(currentRelativeURI ->
		{
			UI.getCurrent().getPage()
				.executeJs("return window.sessionStorage.getItem($0);", key)
				.then(String.class, storedValue -> consumer.consume(storedValue, currentRelativeURI));
		});
	}

	public interface StoredValueConsumer
	{
		void consume(String storedValue, URL currentRelativeURI);
	}
}
