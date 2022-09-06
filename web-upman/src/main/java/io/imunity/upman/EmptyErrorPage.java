/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;

import javax.servlet.http.HttpServletResponse;

@Tag(Tag.DIV)
public class EmptyErrorPage extends Component implements HasErrorParameter<IllegalAccessException>
{
	@Override
	public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<IllegalAccessException> parameter)
	{
		return HttpServletResponse.SC_FORBIDDEN;
	}
}
