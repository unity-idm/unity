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
	public static final String SELECTED_AUTHN_STORAGE_KEY = "uy-select-authn";
	

	public static void consumeSelectedAuthn(StoredValueConsumer consumer)
	{
		consumeSessionStorageItem(SELECTED_AUTHN_STORAGE_KEY, consumer);
	}
	
	public static void consumeRedirectUrl(StoredValueConsumer consumer)
	{
		consumeSessionStorageItem(REDIRECT_URL_SESSION_STORAGE_KEY, consumer);
	}
	
	private static void consumeSessionStorageItem(String key, StoredValueConsumer consumer)
	{
		UI.getCurrent().getPage().fetchCurrentURL(currentRelativeURI ->
		{
			UI.getCurrent().getPage()
			.executeJs(
			        """
			        const v = window.sessionStorage.getItem($0);
			        return v === "null" ? null : v;
			        """,
			        key
			    )
				.then(String.class, storedValue -> consumer.consume(storedValue, currentRelativeURI));
		});
	}

	public interface StoredValueConsumer
	{
		void consume(String storedValue, URL currentRelativeURI);
	}
}
