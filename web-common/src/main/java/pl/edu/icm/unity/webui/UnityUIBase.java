/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.webui;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.authn.CancelHandler;
import pl.edu.icm.unity.webui.common.ErrorPopup;

import com.vaadin.server.DefaultErrorHandler;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.UI;

/**
 * All Unity {@link UI}s should extend this class. It provides a common logic. 
 * Currently proper error handling of unchecked exceptions.
 * @author K. Benedyczak
 */
public abstract class UnityUIBase extends UI implements UnityWebUI
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, UnityUIBase.class);
	
	protected UnityMessageSource msg;
	protected CancelHandler cancelHandler;
	
	public UnityUIBase(UnityMessageSource msg)
	{
		super();
		this.msg = msg;
	}

	@Override
	protected final void init(VaadinRequest request)
	{
		setErrorHandler(new ErrorHandlerImpl());
		appInit(request);
	}


	@Override
	public void setCancelHandler(CancelHandler handler)
	{
		this.cancelHandler = handler;
	}
	
	/**
	 * Same as Vaadin's {@link #init(VaadinRequest)}, separated so 
	 * it is not possible to forgot to call super.init().
	 * @param request
	 */
	protected abstract void appInit(VaadinRequest request);
	
	private class ErrorHandlerImpl extends DefaultErrorHandler 
	{
		@Override
		public void error(com.vaadin.server.ErrorEvent event) {
			
			log.error("UI code got an unchecked and not handled properly exception: " 
					+ event.getThrowable(), event.getThrowable());
			
			ErrorPopup.showError(msg, msg.getMessage("error"), 
					msg.getMessage("UnityUIBase.unhandledError"));
		} 
	}
}
