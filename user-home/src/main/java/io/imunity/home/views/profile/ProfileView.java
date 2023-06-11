/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.home.views.profile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.security.PermitAll;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

import io.imunity.home.HomeEndpointProperties;
import io.imunity.home.views.HomeUiMenu;
import io.imunity.home.views.HomeViewComponent;
import io.imunity.vaadin.auth.additional.AdditionalAuthnHandler;
import io.imunity.vaadin.auth.sandbox.SandboxWizardDialog;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.VaddinWebLogoutHandler;
import io.imunity.vaadin.endpoint.common.api.AssociationAccountWizardProvider;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeEditContext;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeHandlerRegistry;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeViewer;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeViewerContext;
import io.imunity.vaadin.endpoint.common.plugins.attributes.FixedAttributeEditor;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeSupport;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.session.AdditionalAuthenticationMisconfiguredException;
import pl.edu.icm.unity.engine.api.session.AdditionalAuthenticationRequiredException;
import pl.edu.icm.unity.webui.common.ConfirmationEditMode;
import pl.edu.icm.unity.webui.common.FormValidationException;

@PermitAll
@RouteAlias(value = "/", layout = HomeUiMenu.class)
@Route(value = "/profile", layout = HomeUiMenu.class)
public class ProfileView extends HomeViewComponent
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, ProfileView.class);

	private final AttributesManagement attributesMan;
	private final MessageSource msg;
	private final AttributeHandlerRegistry attributeHandlerRegistry;
	private final AdditionalAuthnHandler additionalAuthnHandler;
	private final AssociationAccountWizardProvider associationAccountWizardProvider;
	private final VaddinWebLogoutHandler authnProcessor;
	private final EntityManagement idsMan;
	private final EntityManagement insecureIdsMan;
	private final AttributeSupport atMan;
	private final NotificationPresenter notificationPresenter;

	private List<FixedAttributeEditor> attributeEditors;
	private HomeEndpointProperties config = ComponentUtil.getData(UI.getCurrent(), HomeEndpointProperties.class);
	private LoginSession theUser = InvocationContext.getCurrent().getLoginSession();

	public ProfileView(AttributesManagement attributesMan, MessageSource msg, AttributeHandlerRegistry attributeHandlerRegistry,
					   AdditionalAuthnHandler additionalAuthnHandler, AssociationAccountWizardProvider associationAccountWizardProvider,
					   VaddinWebLogoutHandler authnProcessor1, EntityManagement idsMan, @Qualifier("insecure") EntityManagement insecureIdsMan,
					   AttributeSupport atMan, NotificationPresenter notificationPresenter)
	{
		this.attributesMan = attributesMan;
		this.msg = msg;
		this.attributeHandlerRegistry = attributeHandlerRegistry;
		this.additionalAuthnHandler = additionalAuthnHandler;
		this.associationAccountWizardProvider = associationAccountWizardProvider;
		this.authnProcessor = authnProcessor1;
		this.idsMan = idsMan;
		this.insecureIdsMan = insecureIdsMan;
		this.atMan = atMan;
		this.notificationPresenter = notificationPresenter;
	}

	@Override
	public void afterNavigation(AfterNavigationEvent event)
	{
		init();
	}

	private void init()
	{
		attributeEditors = new ArrayList<>();
		getContent().removeAll();
		config = ComponentUtil.getData(UI.getCurrent(), HomeEndpointProperties.class);
		theUser = InvocationContext.getCurrent().getLoginSession();
		Set<String> keys = config.getStructuredListKeys(HomeEndpointProperties.ATTRIBUTES);

		addAttributes(keys);

		HorizontalLayout buttonsLayout = createButtonsLayout();
		getContent().add(buttonsLayout);
	}

	private HorizontalLayout createButtonsLayout()
	{
		HorizontalLayout buttonsLayout = new HorizontalLayout();
		buttonsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
		buttonsLayout.setMargin(true);

		Button save = new Button(msg.getMessage("save"));
		save.setIcon(VaadinIcon.DISC.create());
		save.addClickListener(event -> saveChanges());
		save.setVisible(shouldShowSave());

		Button refresh = new Button(msg.getMessage("refresh"));
		refresh.setIcon(VaadinIcon.REFRESH.create());
		refresh.addClickListener(e -> init());

		EntityRemovalButton entityRemovalButton = new EntityRemovalButton(msg, theUser.getEntityId(), idsMan, insecureIdsMan, authnProcessor, notificationPresenter, config);
		HorizontalLayout endButtonsLayout = new HorizontalLayout();
		endButtonsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
		endButtonsLayout.add(entityRemovalButton);
		buttonsLayout.add(new HorizontalLayout(save, refresh), endButtonsLayout);

		if (!config.getDisabledComponents().contains(HomeEndpointProperties.Components.accountLinking.toString()))
		{
			Button associationButton = new Button(msg.getMessage("EntityDetailsWithActions.associateAccount"));
			associationButton.setId("EntityDetailsWithActions.associateAccount");
			associationButton.addClickListener(e ->
			{
				SandboxWizardDialog dialog = new SandboxWizardDialog();
				Runnable finishTask = () -> {
					notificationPresenter.showSuccess(
							msg.getMessage("ConnectId.ConfirmStep.mergeSuccessfulCaption"),
							msg.getMessage("ConnectId.ConfirmStep.mergeSuccessful")
					);
					dialog.close();
					init();
				};
				dialog.add(associationAccountWizardProvider.getWizardForConnectId(finishTask, dialog::close));
				dialog.setHeaderTitle(msg.getMessage("ConnectId.wizardCaption"));
				dialog.open();
			});
			endButtonsLayout.add(associationButton);
		}
		return buttonsLayout;
	}

	private void addAttributes(Set<String> keys)
	{
		Map<String, AttributeType> atTypes;
		Set<String> groups;
		try
		{
			atTypes = atMan.getAttributeTypesAsMap();
			groups = idsMan.getGroupsForPresentation(new EntityParam(theUser.getEntityId())).
					stream().map(Group::toString).collect(Collectors.toSet());
		} catch (EngineException e)
		{
			throw new RuntimeException(e);
		}
		VerticalLayout verticalLayout = new VerticalLayout();
		for (String aKey: keys)
		{
			List<Component> attributes = getAttributes(atTypes, aKey, groups);
			attributes.forEach(attr -> ((HasStyle)attr).getStyle().set("width", "20em"));
			verticalLayout.add(attributes);
		}
		getContent().add(verticalLayout);
	}

	private boolean shouldShowSave()
	{
		boolean showSave = false;
		if (!config.getDisabledComponents().contains(HomeEndpointProperties.Components.attributesManagement.toString()))
			showSave = attributeEditors.size() > 0;
		return showSave;
	}

	private List<Component> getAttributes(Map<String, AttributeType> atTypes, String key, Set<String> groups)
	{
		String group = config.getValue(key+ HomeEndpointProperties.GWA_GROUP);
		String attributeName = config.getValue(key+HomeEndpointProperties.GWA_ATTRIBUTE);
		boolean showGroup = config.getBooleanValue(key+HomeEndpointProperties.GWA_SHOW_GROUP);
		boolean editable = config.getBooleanValue(key+HomeEndpointProperties.GWA_EDITABLE);
		AttributeType at = atTypes.get(attributeName);
		if (at == null)
		{
			log.warn("No attribute type " + attributeName + " defined in the system.");
			return List.of();
		}
		AttributeExt attribute = getAttribute(attributeName, group);

		if (!groups.contains(group))
			return List.of();
		if (editable && at.isSelfModificable())
		{

			AttributeEditContext editContext = AttributeEditContext.builder()
					.withConfirmationMode(ConfirmationEditMode.USER)
					.withAttributeType(at)
					.withAttributeGroup(group)
					.withAttributeOwner(new EntityParam(theUser.getEntityId())).build();

			FixedAttributeEditor editor = new FixedAttributeEditor(msg, attributeHandlerRegistry,
					editContext, showGroup, null, null);
			if (attribute != null)
				editor.setAttributeValues(attribute.getValues());
			attributeEditors.add(editor);
			return editor.getComponentsGroup().getComponents();
		} else
		{
			if (attribute == null)
				return List.of();

			AttributeViewer viewer = new AttributeViewer(msg, attributeHandlerRegistry, at,
					attribute, showGroup, AttributeViewerContext.EMPTY);
			return viewer.getComponentsGroup().getComponents();
		}
	}

	public void saveChanges()
	{
		boolean changed = false;
		for (FixedAttributeEditor ae: attributeEditors)
		{
			try
			{
				if (!ae.isChanged())
					continue;
				Optional<Attribute> a = ae.getAttribute();
				if (a.isPresent())
					updateAttribute(a.get());
				else
					removeAttribute(ae);
				changed = true;
			} catch (FormValidationException e)
			{
				continue;
			} catch (AdditionalAuthenticationRequiredException additionalAuthn)
			{
				additionalAuthnHandler.handleAdditionalAuthenticationException(additionalAuthn,
						msg.getMessage("UserAttributesPanel.additionalAuthnRequired"),
						msg.getMessage("UserAttributesPanel.additionalAuthnRequiredInfo"),
						this::onAdditionalAuthnForAttributesSave);
				return;
			} catch (AdditionalAuthenticationMisconfiguredException misconfigured)
			{
				notificationPresenter.showError(msg.getMessage("UserAttributesPanel.attributeUpdateError"),
						msg.getMessage("AdditionalAuthenticationMisconfiguredError"));
				return;
			} catch (EngineException e)
			{
				throw new RuntimeException(e);
			}
		}
	}

	private void updateAttribute(Attribute a) throws EngineException
	{
		attributesMan.setAttribute(new EntityParam(theUser.getEntityId()), a);
	}

	private void removeAttribute(FixedAttributeEditor ae) throws EngineException
	{
		try
		{
			attributesMan.removeAttribute(new EntityParam(theUser.getEntityId()),
					ae.getGroup(), ae.getAttributeType().getName());
		} catch (IllegalArgumentException e)
		{
			//OK - attribute already doesn't exist
		}
	}

	private void onAdditionalAuthnForAttributesSave(AdditionalAuthnHandler.AuthnResult result)
	{
		try
		{
			if (result == AdditionalAuthnHandler.AuthnResult.SUCCESS)
			{
				saveChanges();
				init();
			} else if (result == AdditionalAuthnHandler.AuthnResult.ERROR)
			{
				notificationPresenter.showError(msg.getMessage("UserAttributesPanel.attributeUpdateError"),
						msg.getMessage("UserAttributesPanel.additionalAuthnFailed"));
				init();
			}
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("UserAttributesPanel.attributeUpdateError"), e.getMessage());
		}
	}

	private AttributeExt getAttribute(String attributeName, String group)
	{
		Collection<AttributeExt> attributes;
		try
		{
			attributes = attributesMan.getAttributes(
					new EntityParam(theUser.getEntityId()), group, attributeName);
		} catch (EngineException e)
		{
			log.debug("Can not resolve attribute " + attributeName + " for entity", e);
			return null;
		}
		if (attributes.isEmpty())
			return null;
		return attributes.iterator().next();
	}
}
