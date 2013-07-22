/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.samlidp.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.xml.security.utils.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.AuthenticationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.samlidp.FreemarkerHandler;
import pl.edu.icm.unity.samlidp.preferences.SamlPreferences;
import pl.edu.icm.unity.samlidp.preferences.SamlPreferences.SPSettings;
import pl.edu.icm.unity.samlidp.saml.ctx.SAMLAuthnContext;
import pl.edu.icm.unity.samlidp.saml.processor.AuthnResponseProcessor;
import pl.edu.icm.unity.samlidp.web.filter.SamlParseFilter;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.PreferencesManagement;
import pl.edu.icm.unity.server.authn.AuthenticatedEntity;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.endpoint.BindingAuthn;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.webui.UnityUIBase;
import pl.edu.icm.unity.webui.UnityWebUI;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.TopHeaderLight;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
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
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.themes.Reindeer;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import eu.unicore.samly2.exceptions.SAMLRequesterException;
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
public class SamlIdPWebUI extends UnityUIBase implements UnityWebUI
{
	private static Logger log = Log.getLogger(Log.U_SERVER_SAML, SamlIdPWebUI.class);
	protected UnityMessageSource msg;
	protected EndpointDescription endpointDescription;
	protected IdentitiesManagement identitiesMan;
	protected AttributesManagement attributesMan;
	protected FreemarkerHandler freemarkerHandler;
	protected AttributeHandlerRegistry handlersRegistry;
	protected PreferencesManagement preferencesMan;
	
	protected AuthnResponseProcessor samlProcessor;
	protected List<Identity> validIdentities;
	protected Identity selectedIdentity;
	protected Map<String, Attribute<?>> attributes;
	protected List<CheckBox> hide;
	protected CheckBox rememberCB;
	protected ComboBox identitiesCB;

	@Autowired
	public SamlIdPWebUI(UnityMessageSource msg, IdentitiesManagement identitiesMan,
			AttributesManagement attributesMan, FreemarkerHandler freemarkerHandler,
			AttributeHandlerRegistry handlersRegistry, PreferencesManagement preferencesMan)
	{
		super(msg);
		this.msg = msg;
		this.identitiesMan = identitiesMan;
		this.freemarkerHandler = freemarkerHandler;
		this.attributesMan = attributesMan;
		this.handlersRegistry = handlersRegistry;
		this.preferencesMan = preferencesMan;
	}

	@Override
	public void configure(EndpointDescription description,
			List<Map<String, BindingAuthn>> authenticators)
	{
		this.endpointDescription = description;
	}
	
	protected static SAMLAuthnContext getContext()
	{
		WrappedSession httpSession = VaadinSession.getCurrent().getSession();
		SAMLAuthnContext ret = (SAMLAuthnContext) httpSession.getAttribute(SamlParseFilter.SESSION_SAML_CONTEXT);
		if (ret == null)
			throw new IllegalStateException("No SAML context in UI");
		return ret;
	}
	
	protected static void cleanContext()
	{
		VaadinSession vSession = VaadinSession.getCurrent();
		vSession.setAttribute(ResponseDocument.class, null);
		WrappedSession httpSession = vSession.getSession();
		httpSession.removeAttribute(SamlParseFilter.SESSION_SAML_CONTEXT);
	}
	
	protected Entity getAuthenticatedEntity() throws EngineException
	{
		AuthenticatedEntity ae = InvocationContext.getCurrent().getAuthenticatedEntity();
		return identitiesMan.getEntity(new EntityParam(ae.getEntityId()));
	}
	
	protected Map<String, Attribute<?>> getAttributes(AuthnResponseProcessor processor) 
			throws EngineException
	{
		AuthenticatedEntity ae = InvocationContext.getCurrent().getAuthenticatedEntity();
		EntityParam entity = new EntityParam(ae.getEntityId());
		Collection<String> allGroups = identitiesMan.getGroups(entity);
		Collection<AttributeExt<?>> allAttribtues = attributesMan.getAttributes(
				entity, processor.getChosenGroup(), null);
		return processor.prepareReleasedAttributes(allAttribtues, allGroups);
	}
	
	
	protected Collection<Attribute<?>> getUserFilteredAttributes()
	{
		Set<String> hidden = new HashSet<String>();
		for (CheckBox cb: hide)
			if (cb.getValue())
				hidden.add((String) cb.getData());
		
		List<Attribute<?>> ret = new ArrayList<Attribute<?>>(attributes.size());
		for (Attribute<?> a: attributes.values())
			if (!hidden.contains(a.getName()))
				ret.add(a);
		return ret;
	}
	
	
	@Override
	protected void appInit(VaadinRequest request)
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
		
		try
		{
			createInfoPart(samlCtx, contents);

			createExposedDataPart(contents);

			createButtonsPart(contents);

			setContent(vmain);

			loadPreferences(samlCtx);
		} catch (EopException e)
		{
			//OK
		}
	}

	protected void createInfoPart(SAMLAuthnContext samlCtx, VerticalLayout contents)
	{
		String samlRequester = samlCtx.getRequest().getIssuer().getStringValue();
		String returnAddress = samlCtx.getRequest().getAssertionConsumerServiceURL();
		
		Label info1 = new Label(msg.getMessage("SamlIdPWebUI.info1"));
		info1.setStyleName(Reindeer.LABEL_H1);
		Label info1Id = new Label(msg.getMessage("SamlIdPWebUI.info1Id", samlRequester));
		info1Id.setStyleName(Reindeer.LABEL_H2);
		Label info1Addr = new Label(msg.getMessage("SamlIdPWebUI.info1Addr", returnAddress));
		info1Addr.setStyleName(Reindeer.LABEL_H2);
		Label spc1 = new Label("<br>", ContentMode.HTML);
		Label info2 = new Label(msg.getMessage("SamlIdPWebUI.info2"));
		Label info3 = new Label(msg.getMessage("SamlIdPWebUI.info3"));
		info3.setStyleName(Reindeer.LABEL_SMALL);
		
		contents.addComponents(info1, info1Id, info1Addr, spc1, info2, info3);
	}

	protected void createExposedDataPart(VerticalLayout contents) throws EopException
	{
		Panel exposedInfoPanel = new Panel();
		contents.addComponent(exposedInfoPanel);
		VerticalLayout eiLayout = new VerticalLayout();
		eiLayout.setMargin(true);
		eiLayout.setSpacing(true);
		exposedInfoPanel.setContent(eiLayout);
		try
		{
			createIdentityPart(eiLayout);
			eiLayout.addComponent(new Label("<br>", ContentMode.HTML));
			createAttributesPart(eiLayout);
		} catch (SAMLRequesterException e)
		{
			//we kill the session as the user may want to log as different user if has access to several entities.
			log.debug("SAML problem when handling client request", e);
			handleException(e, true);
			return;
		} catch (Exception e)
		{
			log.error("Engine problem when handling client request", e);
			//we kill the session as the user may want to log as different user if has access to several entities.
			handleException(e, true);
			return;
		}
		
		rememberCB = new CheckBox("Remember the settings for this service and do not show this dialog again");
		contents.addComponent(rememberCB);
	}
	
	protected void createIdentityPart(VerticalLayout contents) throws EngineException, SAMLRequesterException
	{
		Entity authenticatedEntity = getAuthenticatedEntity();
		validIdentities = samlProcessor.getCompatibleIdentities(authenticatedEntity);
		selectedIdentity = validIdentities.get(0);
		if (validIdentities.size() == 1)
		{
			Label identityL = new Label(msg.getMessage("SamlIdPWebUI.identity"));
			identityL.setStyleName(Styles.bold.toString());
			TextField identityTF = new TextField();
			identityTF.setValue(selectedIdentity.toPrettyString());
			identityTF.setReadOnly(true);
			identityTF.setWidth(100, Unit.PERCENTAGE);
			contents.addComponents(identityL, identityTF);
		} else
		{
			Label identitiesL = new Label(msg.getMessage("SamlIdPWebUI.identities")); 
			identitiesL.setStyleName(Styles.bold.toString());
			Label infoManyIds = new Label(msg.getMessage("SamlIdPWebUI.infoManyIds"));
			infoManyIds.setStyleName(Reindeer.LABEL_SMALL);
			identitiesCB = new ComboBox();
			for (Identity id: validIdentities)
				identitiesCB.addItem(id);
			identitiesCB.setImmediate(true);
			identitiesCB.select(selectedIdentity);
			identitiesCB.setNullSelectionAllowed(false);
			identitiesCB.addValueChangeListener(new ValueChangeListener()
			{
				@Override
				public void valueChange(ValueChangeEvent event)
				{
					selectedIdentity = (Identity) identitiesCB.getValue();
				}
			});
			contents.addComponents(identitiesL, infoManyIds, identitiesCB);
		}
	}
	
	protected void createAttributesPart(VerticalLayout contents) throws EngineException
	{
		attributes = getAttributes(samlProcessor);
		Label attributesL = new Label(msg.getMessage("SamlIdPWebUI.attributes"));
		attributesL.setStyleName(Styles.bold.toString());
		Label attributesInfo = new Label(msg.getMessage("SamlIdPWebUI.attributesInfo"));
		attributesInfo.setStyleName(Reindeer.LABEL_SMALL);
		attributesInfo.setContentMode(ContentMode.HTML);
		Label hideL = new Label(msg.getMessage("SamlIdPWebUI.hide"));
		
		contents.addComponent(attributesL);
		contents.addComponent(attributesInfo);
		GridLayout gl = new GridLayout(2, attributes.size()+1);
		gl.setSpacing(true);
		gl.setWidth(100, Unit.PERCENTAGE);
		gl.addComponent(hideL, 1, 0);
		gl.setColumnExpandRatio(0, 10);
		gl.setColumnExpandRatio(1, 1);
		int row=1;
		hide = new ArrayList<CheckBox>(attributes.size());
		for (Attribute<?> at: attributes.values())
		{
			Label attrInfo = new Label();
			String representation = handlersRegistry.getSimplifiedAttributeRepresentation(at, 80);
			attrInfo.setValue(representation);
			gl.addComponent(attrInfo, 0, row);

			CheckBox cb = new CheckBox();
			cb.setData(at.getName());
			cb.setImmediate(true);
			cb.addValueChangeListener(new AttributeHideHandler(attrInfo));
			gl.addComponent(cb, 1, row);
			hide.add(cb);
			row++;
		}
		
		contents.addComponent(gl);
	}
	
	protected void createButtonsPart(VerticalLayout contents)
	{
		HorizontalLayout buttons = new HorizontalLayout();
		
		Button confirmB = new Button(msg.getMessage("SamlIdPWebUI.confirm"));
		confirmB.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				try
				{
					confirm();
				} catch (EopException e)
				{
					//OK
				}
			}
		});
		Button declineB = new Button(msg.getMessage("SamlIdPWebUI.decline"));
		declineB.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				try
				{
					decline();
				} catch (EopException e)
				{
					//OK
				}
			}
		});
		Button reloginB = new Button(msg.getMessage("SamlIdPWebUI.logAsAnother"));
		reloginB.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				WrappedSession ws = VaadinSession.getCurrent().getSession();
				ws.removeAttribute(WebSession.USER_SESSION_KEY);
				Page page = Page.getCurrent();
				URI myUri = page.getLocation();
				page.open(myUri.toASCIIString(), null);
			}
		});
		buttons.addComponents(confirmB, declineB, reloginB);
		buttons.setSpacing(true);
		buttons.setMargin(true);
		buttons.setSizeUndefined();
		contents.addComponent(buttons);
		contents.setComponentAlignment(buttons, Alignment.MIDDLE_CENTER);
	}
	
	
	protected void loadPreferences(SAMLAuthnContext samlCtx) throws EopException
	{
		try
		{
			SamlPreferences preferences = SamlPreferences.getPreferences(preferencesMan);
			String samlRequester = samlCtx.getRequest().getIssuer().getStringValue();
			SPSettings settings = preferences.getSPSettings(samlRequester);
			updateUIFromPreferences(settings, samlCtx);
		} catch (Exception e)
		{
			log.error("Engine problem when processing stored preferences", e);
			//we kill the session as the user may want to log as different user if has access to several entities.
			handleException(e, true);
			return;
		}
	}
	
	protected void updateUIFromPreferences(SPSettings settings, SAMLAuthnContext samlCtx) throws EngineException, EopException
	{
		if (settings == null)
			return;
		Set<String> hidden = settings.getHiddenAttribtues();
		for (CheckBox cb: hide)
		{
			String a = (String) cb.getData();
			if (hidden.contains(a))
				cb.setValue(true);
		}
		if (settings.isDoNotAsk())
		{
			if (settings.isDefaultAccept())
				confirm();
			else
				decline();
		}
		if (validIdentities.size() > 0)
		{
			for (Identity id: validIdentities)
			{
				if (id.getComparableValue().equals(settings.getSelectedIdentity()))
				{
					identitiesCB.select(id);
					selectedIdentity = id;
					break;
				}
			}
		}
	}
	
	/**
	 * Applies UI selected values to the given preferences object
	 * @param preferences
	 * @param samlCtx
	 * @param defaultAccept
	 * @throws EngineException
	 */
	protected void updatePreferencesFromUI(SamlPreferences preferences, SAMLAuthnContext samlCtx, boolean defaultAccept) 
			throws EngineException
	{
		if (!rememberCB.getValue())
			return;
		String samlRequester = samlCtx.getRequest().getIssuer().getStringValue();
		SPSettings settings = preferences.getSPSettings(samlRequester);
		settings.setDefaultAccept(defaultAccept);
		settings.setDoNotAsk(true);
		Set<String> hidden = new HashSet<String>();
		for (CheckBox h: hide)
		{
			if (!h.getValue())
				continue;
			String a = (String) h.getData();
			hidden.add(a);
		}
		settings.setHiddenAttribtues(hidden);
		settings.setSelectedIdentity(selectedIdentity.getComparableValue());
	}
	
	protected void storePreferences(boolean defaultAccept)
	{
		try
		{
			SAMLAuthnContext samlCtx = getContext();
			SamlPreferences preferences = SamlPreferences.getPreferences(preferencesMan);
			updatePreferencesFromUI(preferences, samlCtx, defaultAccept);
			SamlPreferences.savePreferences(preferencesMan, preferences);
		} catch (EngineException e)
		{
			log.error("Unable to store user's preferences", e);
		}
	}

	protected void decline() throws EopException
	{
		storePreferences(false);
		AuthenticationException ea = new AuthenticationException("Authentication was declined");
		handleException(ea, false);
	}
	
	protected void confirm() throws EopException
	{
		storePreferences(true);
		ResponseDocument respDoc;
		try
		{
			Collection<Attribute<?>> attributes = getUserFilteredAttributes();
			respDoc = samlProcessor.processAuthnRequest(selectedIdentity, attributes);
		} catch (Exception e)
		{
			handleException(e, false);
			return;
		}
		returnSamlResponse(respDoc);
	}
	
	protected void handleException(Exception e, boolean destroySession) throws EopException
	{
		SAMLServerException convertedException = samlProcessor.convert2SAMLError(e, null, true);
		ResponseDocument respDoc = samlProcessor.getErrorResponse(convertedException);
		returnSamlErrorResponse(respDoc, convertedException, destroySession);
		throw new EopException();
	}
	
	protected void returnSamlErrorResponse(ResponseDocument respDoc, SAMLServerException error, boolean destroySession)
	{
		VaadinSession.getCurrent().setAttribute(SessionDisposal.class, 
				new SessionDisposal(error, destroySession));
		VaadinSession.getCurrent().setAttribute(SAMLServerException.class, error);
		returnSamlResponse(respDoc);
	}
	
	protected void returnSamlResponse(ResponseDocument respDoc)
	{
		VaadinSession.getCurrent().setAttribute(ResponseDocument.class, respDoc);
		String thisAddress = endpointDescription.getContextAddress() + SamlIdPWebEndpointFactory.SERVLET_PATH;
		VaadinSession.getCurrent().addRequestHandler(new SendResponseRequestHandler());
		Page.getCurrent().open(thisAddress, null);		
	}
	
	/**
	 * This handler intercept all messages and checks if there is a SAML response in the session.
	 * If it is present then the appropriate Freemarker page is rendered which redirects the user's browser 
	 * back to the requesting SP.
	 * @author K. Benedyczak
	 */
	public class SendResponseRequestHandler implements RequestHandler
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
			response.setContentType("application/xhtml+xml; charset=utf-8");
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
	
	private class AttributeHideHandler implements ValueChangeListener
	{
		private Label associatedLabel;
		
		public AttributeHideHandler(Label associatedLabel)
		{
			this.associatedLabel = associatedLabel;
		}

		@Override
		public void valueChange(ValueChangeEvent event)
		{
			Boolean value = (Boolean)event.getProperty().getValue();
			associatedLabel.setEnabled(!value);
		}
	}
}
