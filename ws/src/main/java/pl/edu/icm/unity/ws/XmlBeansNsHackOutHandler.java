/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ws;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.xmlbeans.XmlBeansDataBinding;

/**
 * Turns on the infameous XMLBEANS NAMESPACES HACK for CXF. Without this the message returned by the server 
 * can have the namespaces messed up in the case of XMLBeans binding. 
 * 
 * @author K. Benedyczak
 */
public class XmlBeansNsHackOutHandler extends AbstractPhaseInterceptor<Message>
{
	public XmlBeansNsHackOutHandler()
	{
		super(Phase.SETUP);
	}

	@Override
	public void handleMessage(Message message) throws Fault
	{
		message.setContextualProperty(XmlBeansDataBinding.XMLBEANS_NAMESPACE_HACK, Boolean.TRUE);
	}
}
