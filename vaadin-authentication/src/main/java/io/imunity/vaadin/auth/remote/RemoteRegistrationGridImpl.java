/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth.remote;

import com.vaadin.flow.component.Component;
import io.imunity.vaadin.auth.AuthnsGridWidget;
import io.imunity.vaadin.auth.SearchComponent;
import io.imunity.vaadin.endpoint.common.api.RemoteRegistrationGrid;
import pl.edu.icm.unity.base.message.MessageSource;

import java.util.Objects;


final class RemoteRegistrationGridImpl implements RemoteRegistrationGrid
{
	private final MessageSource msg;
	private final AuthnsGridWidget authnsGridWidget;
	private final boolean empty;

	RemoteRegistrationGridImpl(MessageSource msg, AuthnsGridWidget authnsGridWidget, boolean empty)
	{
		this.msg = msg;
		this.authnsGridWidget = authnsGridWidget;
		this.empty = empty;
	}

	@Override
	public Component getComponent()
	{
		return authnsGridWidget;
	}


	@Override
	public Component getSearchComponent()
	{
		return new SearchComponent(msg, authnsGridWidget::filter);
	}

	@Override
	public boolean isEmpty()
	{
		return empty;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		var that = (RemoteRegistrationGridImpl) obj;
		return Objects.equals(this.msg, that.msg) &&
				Objects.equals(this.authnsGridWidget, that.authnsGridWidget) &&
				this.empty == that.empty;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(msg, authnsGridWidget, empty);
	}

	@Override
	public String toString()
	{
		return "RemoteRegistrationGridImpl[" +
				"msg=" + msg + ", " +
				"authnsGridWidget=" + authnsGridWidget + ", " +
				"empty=" + empty + ']';
	}

}
