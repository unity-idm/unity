/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.front;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.ParentLayout;
import io.imunity.upman.front.views.UpManMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.lang.invoke.MethodHandles;

@ParentLayout(UpManMenu.class)
public class UpmanErrorPage extends UnityViewComponent implements HasErrorParameter<Exception>
{
	private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<Exception> parameter)
	{
		LOG.error("This error occurred, when vaadin page has been rendered:", parameter.getCaughtException());
		getElement().setText("Error");
		return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
	}

	@Override
	public void loadData()
	{
	}
}
