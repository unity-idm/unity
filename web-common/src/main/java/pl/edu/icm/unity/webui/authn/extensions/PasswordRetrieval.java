/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.extensions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vaadin.server.Resource;
import com.vaadin.server.UserError;
import com.vaadin.ui.Component;
import com.vaadin.ui.PasswordField;

import eu.unicore.util.configuration.ConfigurationException;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.RuntimeEngineException;
import pl.edu.icm.unity.server.authn.AuthenticatedEntity;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.server.authn.CredentialExchange;
import pl.edu.icm.unity.server.authn.CredentialRetrieval;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.stdext.credential.PasswordExchange;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;

/**
 * Retrieves passwords using a Vaadin widget.
 * 
 * @author K. Benedyczak
 */
public class PasswordRetrieval implements CredentialRetrieval, VaadinAuthentication
{
	private UsernameProvider usernameProvider;
	private PasswordExchange credentialExchange;
	private PasswordField passwordField;
	private UnityMessageSource msg;
	private String name;
	
	public PasswordRetrieval(UnityMessageSource msg)
	{
		this.msg = msg;
	}
	
	@Override
	public String getBindingName()
	{
		return VaadinAuthentication.NAME;
	}

	@Override
	public String getSerializedConfiguration()
	{
		ObjectNode root = Constants.MAPPER.createObjectNode();
		root.put("name", name);
		try
		{
			return Constants.MAPPER.writeValueAsString(root);
		} catch (JsonProcessingException e)
		{
			throw new RuntimeEngineException("Can't serialize web-based password retrieval configuration to JSON", e);
		}
	}

	@Override
	public void setSerializedConfiguration(String json)
	{
		try
		{
			JsonNode root = Constants.MAPPER.readTree(json);
			name = root.get("name").asText();
		} catch (Exception e)
		{
			throw new ConfigurationException("The configuration of the web-" +
					"based password retrieval can not be parsed", e);
		}
	}

	@Override
	public void setCredentialExchange(CredentialExchange e)
	{
		this.credentialExchange = (PasswordExchange) e;
	}

	@Override
	public boolean needsCommonUsernameComponent()
	{
		return true;
	}

	@Override
	public Component getComponent()
	{
		String label = name.trim().equals("") ? msg.getMessage("PasswordRetrieval.password") : name;
		passwordField = new PasswordField(name);
		return passwordField;
	}

	@Override
	public void setUsernameCallback(UsernameProvider usernameCallback)
	{
		this.usernameProvider = usernameCallback;
	}

	@Override
	public AuthenticationResult getAuthenticationResult()
	{
		String username = usernameProvider.getUsername();
		String password = passwordField.getValue();
		if (username.equals("") && password.equals(""))
		{
			passwordField.setComponentError(new UserError(
					msg.getMessage("PasswordRetrieval.noPassword")));
			return new AuthenticationResult(Status.notApplicable, null);
		}
		try
		{
			AuthenticatedEntity authenticatedEntity = credentialExchange.checkPassword(username, password);
			return new AuthenticationResult(Status.success, authenticatedEntity);
		} catch (Exception e)
		{
			passwordField.setComponentError(new UserError(
					msg.getMessage("PasswordRetrieval.wrongPassword")));
			passwordField.setValue("");
			return new AuthenticationResult(Status.deny, null);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getLabel()
	{
		return name;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Resource getImage()
	{
		return null;
	}
}










