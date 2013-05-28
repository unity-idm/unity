/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.samlidp.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.xml.security.utils.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.AuthenticationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.samlidp.FreemarkerHandler;
import pl.edu.icm.unity.samlidp.saml.ctx.SAMLAuthnContext;
import pl.edu.icm.unity.samlidp.saml.processor.AuthnResponseProcessor;
import pl.edu.icm.unity.samlidp.web.filter.SamlParseFilter;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.authn.AuthenticatedEntity;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.endpoint.BindingAuthn;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.webui.UnityWebUI;
import pl.edu.icm.unity.webui.common.TopHeaderLight;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

import com.vaadin.annotations.Theme;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.Page;
import com.vaadin.server.RequestHandler;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedSession;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.themes.Reindeer;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import eu.unicore.samly2.exceptions.SAMLServerException;

/**
 * The main UI of the SAML web IdP. Fairly simple: shows who asks, what is going to be sent,
 * and optionally allows for some customization. This UI is shown always after the user was authenticated
 * and when the SAML request was properly pre-processed.
 *  
 * @author K. Benedyczak
 */
@Component("SamlIdPWebUI")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Theme("unityTheme")
public class SamlIdPWebUI extends UI implements UnityWebUI
{
	private UnityMessageSource msg;
	private EndpointDescription endpointDescription;
	private IdentitiesManagement identitiesMan;
	private FreemarkerHandler freemarkerHandler;
	
	private AuthnResponseProcessor samlProcessor;
	private List<Identity> validIdentities;
	private Identity selectedIdentity;

	@Autowired
	public SamlIdPWebUI(UnityMessageSource msg, IdentitiesManagement identitiesMan,
			FreemarkerHandler freemarkerHandler)
	{
		this.msg = msg;
		this.identitiesMan = identitiesMan;
		this.freemarkerHandler = freemarkerHandler;
	}

	@Override
	public void configure(EndpointDescription description,
			List<Map<String, BindingAuthn>> authenticators)
	{
		this.endpointDescription = description;
	}
	
	private static SAMLAuthnContext getContext()
	{
		WrappedSession httpSession = VaadinSession.getCurrent().getSession();
		SAMLAuthnContext ret = (SAMLAuthnContext) httpSession.getAttribute(SamlParseFilter.SESSION_SAML_CONTEXT);
		if (ret == null)
			throw new IllegalStateException("No SAML context in UI");
		return ret;
	}
	
	private static void cleanContext()
	{
		VaadinSession vSession = VaadinSession.getCurrent();
		vSession.setAttribute(ResponseDocument.class, null);
		WrappedSession httpSession = vSession.getSession();
		httpSession.removeAttribute(SamlParseFilter.SESSION_SAML_CONTEXT);
	}
	
	private Entity getAuthenticatedEntity() throws EngineException
	{
		AuthenticatedEntity ae = InvocationContext.getCurrent().getAuthenticatedEntity();
		return identitiesMan.getEntity(new EntityParam(String.valueOf(ae.getEntityId())));
	}
	
	@Override
	protected void init(VaadinRequest request)
	{
		SAMLAuthnContext samlCtx = getContext();
		samlProcessor = new AuthnResponseProcessor(samlCtx, Calendar.getInstance());

		VerticalLayout vmain = new VerticalLayout();
		TopHeaderLight header = new TopHeaderLight(endpointDescription.getId(), msg);
		vmain.addComponent(header);

		
		VerticalLayout contents = new VerticalLayout();
		contents.setSizeUndefined();
		contents.setMargin(true);
		contents.setSpacing(true);
		vmain.addComponent(contents);
		vmain.setComponentAlignment(contents, Alignment.TOP_CENTER);
		
		String samlRequester = samlCtx.getRequest().getIssuer().getStringValue();
		String returnAddress = samlCtx.getRequest().getAssertionConsumerServiceURL();
		
		Label info1 = new Label(msg.getMessage("SamlIdPWebUI.info1"));
		info1.setStyleName(Reindeer.LABEL_H1);
		Label info1Id = new Label(msg.getMessage("SamlIdPWebUI.info1Id", samlRequester));
		info1Id.setStyleName(Reindeer.LABEL_H2);
		Label info1Addr = new Label(msg.getMessage("SamlIdPWebUI.info1Addr", returnAddress));
		info1Addr.setStyleName(Reindeer.LABEL_H2);
		Label spc1 = new Label("");
		spc1.setHeight(20, Unit.PIXELS);
		Label info2 = new Label(msg.getMessage("SamlIdPWebUI.info2"));
		Label info3 = new Label(msg.getMessage("SamlIdPWebUI.info3"));
		Label spc2 = new Label("");
		spc2.setHeight(20, Unit.PIXELS);
		contents.addComponents(info1, info1Id, info1Addr, spc1, info2, info3, spc2);
		
		try
		{
			Entity authenticatedEntity = getAuthenticatedEntity();
			validIdentities = samlProcessor.getCompatibleIdentities(authenticatedEntity);
		} catch (Exception e)
		{
			//we kill the session as the user may want to log as different user if has access to several entities.
			handleException(e, true);
			return;
		}
		
		selectedIdentity = validIdentities.get(0);
		if (validIdentities.size() == 1)
		{
			TextField identityTF = new TextField(msg.getMessage("SamlIdPWebUI.identity"));
			identityTF.setValue(selectedIdentity.toPrettyString());
			identityTF.setReadOnly(true);
			identityTF.setWidth(100, Unit.PERCENTAGE);
			contents.addComponent(identityTF);
		} else
		{
			
			final ComboBox identitiesCB = new ComboBox(msg.getMessage("SamlIdPWebUI.identities"));
			for (Identity id: validIdentities)
				identitiesCB.addItem(id);
			identitiesCB.setImmediate(true);
			identitiesCB.select(selectedIdentity);
			identitiesCB.addValueChangeListener(new ValueChangeListener()
			{
				@Override
				public void valueChange(ValueChangeEvent event)
				{
					selectedIdentity = (Identity) identitiesCB.getValue();
				}
			});
			Label infoManyIds = new Label(msg.getMessage("SamlIdPWebUI.infoManyIds"));
			contents.addComponents(infoManyIds, identitiesCB);
		}
		
		Label attributesInfo = new Label("Attributes:");
		attributesInfo.setEnabled(false);
		contents.addComponent(attributesInfo);
		
		CheckBox rememberCB = new CheckBox("Remember the settings for this service and do not show this dialog again");
		rememberCB.setEnabled(false);
		contents.addComponent(rememberCB);
		
		HorizontalLayout buttons = new HorizontalLayout();
		
		Button confirmB = new Button(msg.getMessage("SamlIdPWebUI.confirm"));
		confirmB.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				confirm();
			}
		});
		Button declineB = new Button(msg.getMessage("SamlIdPWebUI.decline"));
		declineB.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				AuthenticationException ea = new AuthenticationException("Authentication was declined");
				handleException(ea, false);
			}
		});
		buttons.addComponents(confirmB, declineB);
		buttons.setSpacing(true);
		buttons.setMargin(true);
		buttons.setSizeUndefined();
		contents.addComponent(buttons);
		contents.setComponentAlignment(buttons, Alignment.MIDDLE_CENTER);
		
		setContent(vmain);
	}

	private void confirm()
	{
		ResponseDocument respDoc;
		try
		{
			respDoc = samlProcessor.processAuthnRequest(selectedIdentity);
		} catch (Exception e)
		{
			handleException(e, false);
			return;
		}
		returnSamlResponse(respDoc);
	}
	
	private void handleException(Exception e, boolean destroySession)
	{
		SAMLServerException convertedException = samlProcessor.convert2SAMLError(e, null, true);
		ResponseDocument respDoc = samlProcessor.getErrorResponse(convertedException);
		returnSamlErrorResponse(respDoc, convertedException, destroySession);
	}
	
	private void returnSamlErrorResponse(ResponseDocument respDoc, SAMLServerException error, boolean destroySession)
	{
		VaadinSession.getCurrent().setAttribute(SessionDisposal.class, 
				new SessionDisposal(error, destroySession));
		VaadinSession.getCurrent().setAttribute(SAMLServerException.class, error);
		returnSamlResponse(respDoc);
	}
	
	private void returnSamlResponse(ResponseDocument respDoc)
	{
		VaadinSession.getCurrent().setAttribute(ResponseDocument.class, respDoc);
		String thisAddress = endpointDescription.getContextAddress() + SamlAuthVaadinEndpoint.SERVLET_PATH;
		VaadinSession.getCurrent().addRequestHandler(new SendResponseRequestHandler());
		Page.getCurrent().open(thisAddress, null);		
	}
	
	/**
	 * This handler intercept all messages and checks if there is a SAML response in the session.
	 * If it is present then the appropriate Freemarker page is rendered which redirects the user's browser 
	 * back to the requesting SP.
	 * @author K. Benedyczak
	 */
	private class SendResponseRequestHandler implements RequestHandler
	{
		@Override
		public boolean handleRequest(VaadinSession session, VaadinRequest request, VaadinResponse response)
						throws IOException
		{
			ResponseDocument samlResponse = session.getAttribute(ResponseDocument.class);
			if (samlResponse == null)
				return false;
			String assertion = samlResponse.xmlText();
			String encodedAssertion = Base64.encode(assertion.getBytes());
			SessionDisposal error = session.getAttribute(SessionDisposal.class);
			
			SAMLAuthnContext samlCtx = getContext();
			String serviceUrl = samlCtx.getRequestDocument().getAuthnRequest().getAssertionConsumerServiceURL();
			Map<String, String> data = new HashMap<String, String>();
			data.put("SAMLResponse", encodedAssertion);
			data.put("samlService", serviceUrl);
			if (error != null)
				data.put("error", error.getE().getMessage());
			if (samlCtx.getRelayState() != null)
				data.put("RelayState", samlCtx.getRelayState());
			
			cleanContext();
			if (error!= null && error.isDestroySession())
				session.getSession().invalidate();
			PrintWriter writer = response.getWriter();
			freemarkerHandler.process("finishSaml.ftl", data, writer);
			return true;
		}
	}
	
	private static class SessionDisposal
	{
		private SAMLServerException e;
		private boolean destroySession;
		
		public SessionDisposal(SAMLServerException e, boolean destroySession)
		{
			this.e = e;
			this.destroySession = destroySession;
		}

		protected SAMLServerException getE()
		{
			return e;
		}

		protected boolean isDestroySession()
		{
			return destroySession;
		}
	}
}
