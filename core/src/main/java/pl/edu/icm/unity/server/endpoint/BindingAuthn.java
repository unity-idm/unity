/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.endpoint;

/**
 * Interface defining binding specific API (e.g. for Vaadin Web interface or CXF WS)
 * which must be implemented to plug an authentication to the binding. Nearly marker - the real
 * functionality will be available in extensions. 
 * @author K. Benedyczak
 */
public interface BindingAuthn
{
	public String getBindingId();
	public String getBindingName();
	public String getBindingDescription();
}
