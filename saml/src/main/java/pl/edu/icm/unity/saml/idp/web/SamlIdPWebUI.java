/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.web;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.idpcommon.EopException;
import pl.edu.icm.unity.saml.idp.FreemarkerHandler;
import pl.edu.icm.unity.saml.idp.SamlIdpProperties;
import pl.edu.icm.unity.saml.idp.ctx.SAMLAuthnContext;
import pl.edu.icm.unity.saml.idp.preferences.SamlPreferences;
import pl.edu.icm.unity.saml.idp.preferences.SamlPreferences.SPSettings;
import pl.edu.icm.unity.saml.idp.processor.AuthnResponseProcessor;
import pl.edu.icm.unity.server.api.PreferencesManagement;
import pl.edu.icm.unity.server.api.internal.IdPEngine;
import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.authn.AuthenticationException;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.endpoint.BindingAuthn;
import pl.edu.icm.unity.server.translation.out.TranslationResult;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityTypeDefinition;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.webui.EndpointRegistrationConfiguration;
import pl.edu.icm.unity.webui.UnityUIBase;
import pl.edu.icm.unity.webui.UnityWebUI;
import pl.edu.icm.unity.webui.authn.AuthenticationProcessor;
import pl.edu.icm.unity.webui.common.ListOfSelectableElements;
import pl.edu.icm.unity.webui.common.ListOfSelectableElements.DisableMode;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.TopHeaderLight;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

import com.vaadin.annotations.Theme;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Reindeer;

import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.exceptions.SAMLRequesterException;

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
	protected IdPEngine idpEngine;
	protected FreemarkerHandler freemarkerHandler;
	protected AttributeHandlerRegistry handlersRegistry;
	protected PreferencesManagement preferencesMan;
	protected AuthenticationProcessor authnProcessor;
	
	protected AuthnResponseProcessor samlProcessor;
	protected SamlResponseHandler samlResponseHandler;
	protected List<IdentityParam> validIdentities;
	protected IdentityParam selectedIdentity;
	protected Map<String, Attribute<?>> attributes;
	protected ListOfSelectableElements attributesHiding;
	protected CheckBox rememberCB;
	protected ComboBox identitiesCB;

	@Autowired
	public SamlIdPWebUI(UnityMessageSource msg, FreemarkerHandler freemarkerHandler,
			AttributeHandlerRegistry handlersRegistry, PreferencesManagement preferencesMan,
			AuthenticationProcessor authnProcessor, IdPEngine idpEngine)
	{
		super(msg);
		this.msg = msg;
		this.freemarkerHandler = freemarkerHandler;
		this.handlersRegistry = handlersRegistry;
		this.preferencesMan = preferencesMan;
		this.authnProcessor = authnProcessor;
		this.idpEngine = idpEngine;
	}

	@Override
	public void configure(EndpointDescription description,
			List<Map<String, BindingAuthn>> authenticators,
			EndpointRegistrationConfiguration regCfg)
	{
		this.endpointDescription = description;
	}
	
	protected TranslationResult getUserInfo(SAMLAuthnContext samlCtx, AuthnResponseProcessor processor) 
			throws EngineException
	{
		String profile = samlCtx.getSamlConfiguration().getValue(SamlIdpProperties.TRANSLATION_PROFILE);
		LoginSession ae = InvocationContext.getCurrent().getLoginSession();
		return idpEngine.obtainUserInformation(new EntityParam(ae.getEntityId()), 
				processor.getChosenGroup(), profile, 
				samlProcessor.getIdentityTarget(), "SAML2", SAMLConstants.BINDING_HTTP_REDIRECT,
				processor.isIdentityCreationAllowed());
	}

	protected Collection<Attribute<?>> getUserFilteredAttributes()
	{
		Set<String> hidden = new HashSet<String>();
		for (CheckBox cb: attributesHiding.getSelection())
			if (cb.getValue())
				hidden.add((String) cb.getData());
		
		List<Attribute<?>> ret = new ArrayList<Attribute<?>>(attributes.size());
		for (Attribute<?> a: attributes.values())
			if (!hidden.contains(a.getName()))
				ret.add(a);
		return ret;
	}
	
	protected String getMyAddress()
	{
		return endpointDescription.getContextAddress()+SamlIdPWebEndpointFactory.SAML_UI_SERVLET_PATH;
	}
	
	@Override
	protected void appInit(VaadinRequest request)
	{
		SAMLAuthnContext samlCtx = SamlResponseHandler.getContext();
		samlProcessor = new AuthnResponseProcessor(samlCtx, Calendar.getInstance(TimeZone.getTimeZone("UTC")));
		samlResponseHandler = new SamlResponseHandler(freemarkerHandler, samlProcessor, 
				getMyAddress());
		
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

			createExposedDataPart(samlCtx, contents);

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
		if (returnAddress == null)
			returnAddress = samlCtx.getSamlConfiguration().getReturnAddressForRequester(
					samlCtx.getRequest().getIssuer());
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

	protected void createExposedDataPart(SAMLAuthnContext samlCtx, VerticalLayout contents) throws EopException
	{
		Panel exposedInfoPanel = new Panel();
		contents.addComponent(exposedInfoPanel);
		VerticalLayout eiLayout = new VerticalLayout();
		eiLayout.setMargin(true);
		eiLayout.setSpacing(true);
		exposedInfoPanel.setContent(eiLayout);
		try
		{
			TranslationResult translationResult = getUserInfo(samlCtx, samlProcessor);
			createIdentityPart(translationResult, eiLayout);
			eiLayout.addComponent(new Label("<br>", ContentMode.HTML));
			createAttributesPart(translationResult, eiLayout);
		} catch (SAMLRequesterException e)
		{
			//we kill the session as the user may want to log as different user if has access to several entities.
			log.debug("SAML problem when handling client request", e);
			samlResponseHandler.handleException(e, true);
			return;
		} catch (Exception e)
		{
			log.error("Engine problem when handling client request", e);
			//we kill the session as the user may want to log as different user if has access to several entities.
			samlResponseHandler.handleException(e, true);
			return;
		}
		
		rememberCB = new CheckBox("Remember the settings for this service and do not show this dialog again");
		contents.addComponent(rememberCB);
	}
	
	protected void createIdentityPart(TranslationResult translationResult, VerticalLayout contents) 
			throws EngineException, SAMLRequesterException
	{
		validIdentities = samlProcessor.getCompatibleIdentities(translationResult.getIdentities());
		selectedIdentity = validIdentities.get(0);
		if (validIdentities.size() == 1)
		{
			Label identityL = new Label(msg.getMessage("SamlIdPWebUI.identity"));
			identityL.setStyleName(Styles.bold.toString());
			TextField identityTF = new TextField();
			identityTF.setValue(selectedIdentity.getValue());
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
			for (IdentityParam id: validIdentities)
				identitiesCB.addItem(id);
			identitiesCB.setImmediate(true);
			identitiesCB.select(selectedIdentity);
			identitiesCB.setNullSelectionAllowed(false);
			identitiesCB.addValueChangeListener(new ValueChangeListener()
			{
				@Override
				public void valueChange(ValueChangeEvent event)
				{
					selectedIdentity = (IdentityParam) identitiesCB.getValue();
				}
			});
			contents.addComponents(identitiesL, infoManyIds, identitiesCB);
		}
	}
	
	protected void createAttributesPart(TranslationResult translationResult, VerticalLayout contents) throws EngineException
	{
		attributes = new HashMap<String, Attribute<?>>();
		for (Attribute<?> a: translationResult.getAttributes())
			attributes.put(a.getName(), a);
		Label attributesL = new Label(msg.getMessage("SamlIdPWebUI.attributes"));
		attributesL.setStyleName(Styles.bold.toString());
		Label attributesInfo = new Label(msg.getMessage("SamlIdPWebUI.attributesInfo"));
		attributesInfo.setStyleName(Reindeer.LABEL_SMALL);
		attributesInfo.setContentMode(ContentMode.HTML);
		Label hideL = new Label(msg.getMessage("SamlIdPWebUI.hide"));
		
		contents.addComponent(attributesL);
		contents.addComponent(attributesInfo);
		
		attributesHiding = new ListOfSelectableElements(null, hideL, DisableMode.WHEN_SELECTED);
		for (Attribute<?> at: attributes.values())
		{
			Label attrInfo = new Label();
			String representation = handlersRegistry.getSimplifiedAttributeRepresentation(at, 80);
			attrInfo.setValue(representation);
			attributesHiding.addEntry(attrInfo, false, at.getName());
		}
		
		contents.addComponent(attributesHiding);
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
				authnProcessor.logout(true);
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
			SPSettings settings = preferences.getSPSettings(samlCtx.getRequest().getIssuer());
			updateUIFromPreferences(settings, samlCtx);
		} catch (EopException e)
		{
			throw e;
		} catch (Exception e)
		{
			log.error("Engine problem when processing stored preferences", e);
			//we kill the session as the user may want to log as different user if has access to several entities.
			samlResponseHandler.handleException(e, true);
			return;
		}
	}
	
	protected void updateUIFromPreferences(SPSettings settings, SAMLAuthnContext samlCtx) throws EngineException, EopException
	{
		if (settings == null)
			return;
		Set<String> hidden = settings.getHiddenAttribtues();
		for (CheckBox cb: attributesHiding.getSelection())
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
		String selId = settings.getSelectedIdentity();
		if (validIdentities.size() > 0 && selId != null)
		{
			for (IdentityParam id: validIdentities)
			{
				if (id instanceof Identity)
					
				{
					if (((Identity)id).getComparableValue().equals(selId))
					{
						if (identitiesCB != null)
							identitiesCB.select(id);
						selectedIdentity = id;
						break;
					}
				} else if (id.getValue().equals(selId))
				{
					if (identitiesCB != null)
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
		NameIDType reqIssuer = samlCtx.getRequest().getIssuer();
		SPSettings settings = preferences.getSPSettings(reqIssuer);
		settings.setDefaultAccept(defaultAccept);
		settings.setDoNotAsk(true);
		Set<String> hidden = new HashSet<String>();
		for (CheckBox h: attributesHiding.getSelection())
		{
			if (!h.getValue())
				continue;
			String a = (String) h.getData();
			hidden.add(a);
		}
		settings.setHiddenAttribtues(hidden);

		boolean dynamic = false;
		String identityValue = selectedIdentity.getValue();
		if (selectedIdentity instanceof Identity)
		{
			Identity casted = (Identity) selectedIdentity;
			identityValue = casted.getComparableValue();
			IdentityTypeDefinition idType = casted.getType().getIdentityTypeProvider();
			if (idType.isDynamic() || idType.isTargeted())
				dynamic = true;
		}
		if (!dynamic)
			settings.setSelectedIdentity(identityValue);
		preferences.setSPSettings(reqIssuer, settings);
	}
	
	protected void storePreferences(boolean defaultAccept)
	{
		try
		{
			SAMLAuthnContext samlCtx = SamlResponseHandler.getContext();
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
		samlResponseHandler.handleException(ea, false);
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
			samlResponseHandler.handleException(e, false);
			return;
		}
		samlResponseHandler.returnSamlResponse(respDoc);
	}
}
