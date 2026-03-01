/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.console.tokens;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Span;

import io.imunity.vaadin.elements.CssClassNames;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.oauth.as.RequestedOAuthScope;

class GrantedScopeComponent extends Span
{
	GrantedScopeComponent(OAuthToken oauthToken, MessageSource msg)
	{
		List<String> regestedScopeAsList = Stream.of(oauthToken.getRequestedScope())
				.toList();
		List<RequestedOAuthScope> sortedEffectiveScopes = oauthToken.getEffectiveScope()
				.stream()
				.sorted(Comparator.comparingInt(s -> regestedScopeAsList.indexOf(s.scope())))
				.toList();

		Iterator<RequestedOAuthScope> it = sortedEffectiveScopes.iterator();
		while (it.hasNext())
		{
			RequestedOAuthScope scope = it.next();
			NativeLabel scopeLabel = new NativeLabel(scope.scope());
			scopeLabel.addClassName(CssClassNames.MONOSPACE.getName());

			add(scopeLabel);

			if (scope.pattern())
			{
				add(new NativeLabel(msg.getMessage("OAuthTokenViewer.pattern")));
			}

			if (it.hasNext())
			{
				add(new NativeLabel(", "));
			}
		}
	}
}
