/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.home.views.profile;

import static io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeViewerContext.EMPTY;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

import io.imunity.home.HomeEndpointProperties;
import io.imunity.home.views.HomeUiMenu;
import io.imunity.home.views.HomeViewComponent;
import io.imunity.vaadin.auth.additional.AdditionalAuthnHandler;
import io.imunity.vaadin.elements.Breadcrumb;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.VaddinWebLogoutHandler;
import io.imunity.vaadin.endpoint.common.WebSession;
import io.imunity.vaadin.endpoint.common.api.AssociationAccountWizardProvider;
import io.imunity.vaadin.endpoint.common.bus.EventsBus;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeEditContext;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeHandlerRegistry;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeModyficationEvent;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeViewer;
import io.imunity.vaadin.endpoint.common.plugins.attributes.ComponentsGroup;
import io.imunity.vaadin.endpoint.common.plugins.attributes.ConfirmationEditMode;
import io.imunity.vaadin.endpoint.common.plugins.attributes.FixedAttributeEditor;
import io.imunity.vaadin.endpoint.common.plugins.attributes.LabelContext;
import jakarta.annotation.security.PermitAll;
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

@PermitAll
@Breadcrumb(key = "UserHomeUI.profile")
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

	private Button save;
	private Button reset;

	ProfileView(AttributesManagement attributesMan, MessageSource msg, AttributeHandlerRegistry attributeHandlerRegistry,
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

		Set<String> disabledComponents = config.getDisabledComponents();
		VerticalLayout mainLayout = new VerticalLayout();
		HorizontalLayout buttonsLayout = createButtonsLayout(mainLayout, disabledComponents);		
		if (!disabledComponents.contains(HomeEndpointProperties.Components.attributesManagement.toString()))
		{
			mainLayout = getAttributes(keys);
		}

		getContent().add(mainLayout);
		getContent().add(buttonsLayout);
	}

	private HorizontalLayout createButtonsLayout(VerticalLayout mainLayout, Set<String> disabledComponents)
	{
		HorizontalLayout buttonsLayout = new HorizontalLayout();
		buttonsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
		buttonsLayout.setMargin(true);

		save = new Button(msg.getMessage("save"));
		save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		save.setIcon(VaadinIcon.DISC.create());
		save.addClickListener(event -> saveChanges());
		save.setVisible(false);

		reset = new Button(msg.getMessage("ProfileView.reset"));
		reset.setIcon(VaadinIcon.REFRESH.create());
		reset.addClickListener(event -> init());
		reset.setVisible(false);

		HorizontalLayout endButtonsLayout = new HorizontalLayout();
		endButtonsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
		buttonsLayout.add(new HorizontalLayout(save, reset), endButtonsLayout);

		if (!disabledComponents.contains(HomeEndpointProperties.Components.accountRemoval.toString()))
		{
			EntityRemovalButton entityRemovalButton =
					new EntityRemovalButton(msg, theUser.getEntityId(), idsMan, insecureIdsMan, authnProcessor, notificationPresenter, config);
			endButtonsLayout.add(entityRemovalButton);
		}

		if (!disabledComponents.contains(HomeEndpointProperties.Components.accountLinking.toString()))
		{
			Button associationButton = new Button(msg.getMessage("EntityDetailsWithActions.associateAccount"));
			associationButton.setId("EntityDetailsWithActions.associateAccount");
			associationButton.addClickListener(e ->
			{
				Runnable finishTask = () -> {
					notificationPresenter.showSuccess(
							msg.getMessage("ConnectId.ConfirmStep.mergeSuccessfulCaption"),
							msg.getMessage("ConnectId.ConfirmStep.mergeSuccessful")
					);
					init();
				};
				associationAccountWizardProvider.getWizardForConnectId(finishTask).open();
				
			});
			endButtonsLayout.add(associationButton);
		}
		return buttonsLayout;
	}

	private VerticalLayout getAttributes(Set<String> keys)
	{
		Map<String, AttributeType> atTypes;
		Set<Group> groups;
		try
		{
			atTypes = atMan.getAttributeTypesAsMap();
			groups = new HashSet<>(idsMan.getGroupsForPresentation(new EntityParam(theUser.getEntityId())));
		} catch (EngineException e)
		{
			throw new RuntimeException(e);
		}
		VerticalLayout verticalLayout = new VerticalLayout();
		for (String aKey: keys)
		{
			List<Component> attributes = getAttributes(atTypes, aKey, groups, verticalLayout);
			if(attributes.isEmpty())
				continue;
			attributes.forEach(ProfileView::setAttributeWidth);
			VerticalLayout innerLayout = new VerticalLayout();
			innerLayout.add(attributes);
			innerLayout.setPadding(false);
			innerLayout.setSpacing(false);
			verticalLayout.add(innerLayout);
		}
		EventsBus eventBus = WebSession.getCurrent().getEventBus();
		eventBus.addListener(event -> refreshButtons(), AttributeModyficationEvent.class);
		
		return verticalLayout;
	}

	private void refreshButtons()
	{
		boolean savable = iSavable();
		save.setVisible(savable);
		reset.setVisible(savable);
	}

	private static Style setAttributeWidth(HasStyle attr)
	{
		return attr.getStyle().set("width", "20em");
	}

	private List<Component> getAttributes(Map<String, AttributeType> atTypes, String key, Set<Group> groups, VerticalLayout layout)
	{
		String groupPath = config.getValue(key+ HomeEndpointProperties.GWA_GROUP);
		String attributeName = config.getValue(key+HomeEndpointProperties.GWA_ATTRIBUTE);
		boolean showGroup = config.getBooleanValue(key+HomeEndpointProperties.GWA_SHOW_GROUP);
		boolean editable = config.getBooleanValue(key+HomeEndpointProperties.GWA_EDITABLE);
		AttributeType attributeType = atTypes.get(attributeName);
		if (attributeType == null)
		{
			log.warn("No attribute type " + attributeName + " defined in the system.");
			return List.of();
		}
		AttributeExt attribute = getAttribute(attributeName, groupPath);

		Optional<Group> group = groups.stream().filter(grp -> grp.toString().equals(groupPath)).findFirst();
		if (group.isEmpty())
			return List.of();
		LabelContext labelContext = new LabelContext(
				attributeType.getDisplayedName().getValue(msg),
				showGroup,
				groupPath,
				group.get().getDisplayedName().getValue(msg));
		if (editable && attributeType.isSelfModificable())
		{
			AttributeEditContext editContext = AttributeEditContext.builder()
					.withConfirmationMode(ConfirmationEditMode.USER)
					.withAttributeType(attributeType)
					.withAttributeGroup(groupPath)
					.withLabelContext(labelContext)
					.withAttributeOwner(new EntityParam(theUser.getEntityId())).build();

			FixedAttributeEditor editor = new FixedAttributeEditor(msg, attributeHandlerRegistry,
					editContext, labelContext, null);
			editor.addValueChangeListener(() -> refreshButtons());
			if (attribute != null)
				editor.setAttributeValues(attribute.getValues());
			attributeEditors.add(editor);
			ComponentsGroup componentsGroup = editor.getComponentsGroup();
			componentsGroup.setAfterComponentInsertionListener((comp, before) ->
					findLayout(layout, before).ifPresent(innerLayout ->
					{
						innerLayout.addComponentAtIndex(innerLayout.indexOf(before) + 1, comp);
						setAttributeWidth(comp);
					})
			);
			componentsGroup.setComponentRemovalListener(component ->
					findLayout(layout, component).ifPresent(innerLayout -> innerLayout.remove(component))
			);
			return componentsGroup.getComponents();
		} else
		{
			if (attribute == null)
				return List.of();

			AttributeViewer viewer = new AttributeViewer(msg, attributeHandlerRegistry, attributeType,
					attribute, labelContext, EMPTY);
			return viewer.getComponentsGroup().getComponents();
		}
	}

	private static Optional<VerticalLayout> findLayout(VerticalLayout layout, Component before)
	{
		for(int i = 0; i < layout.getComponentCount(); i++)
		{
			if(layout.getComponentAt(i) instanceof VerticalLayout innerLayout)
			{
				int index = innerLayout.indexOf(before);
				if(index != -1)
					return Optional.of(innerLayout);
			}
		}
		return Optional.empty();
	}

	public boolean iSavable()
	{
		for (FixedAttributeEditor ae: attributeEditors)
		{
			try
			{
				if (ae.isChanged())
					return true;
			} catch (FormValidationException e)
			{
				log.warn(e);
			}
		}
		return false;
	}

	public void saveChanges()
	{
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
			} catch (FormValidationException ignored)
			{
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
		notificationPresenter.showSuccess(msg.getMessage("ProfileView.saved"));
		init();
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
