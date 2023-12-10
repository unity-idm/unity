/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities;

import com.vaadin.flow.component.icon.VaadinIcon;
import io.imunity.console.views.directory_browser.group_browser.GroupManagementHelper;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.grid.SingleActionHandler;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeHandlerRegistry;
import io.imunity.vaadin.endpoint.common.plugins.identities.IdentityEditorRegistry;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.CredentialRequirementManagement;
import pl.edu.icm.unity.engine.api.EntityCredentialManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeSupport;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Component
class EntityCreationHandler
{
	private final MessageSource msg;
	private final EntityManagement identitiesMan;
	private final CredentialRequirementManagement credReqMan;
	private final GroupManagementHelper groupHelper;
	private final AttributeTypeManagement attrMan;
	private final IdentityEditorRegistry identityEditorReg;
	private final AttributeSupport attributeSupport;
	private final AttributeHandlerRegistry attributeHandlerRegistry;
	private final NewEntityCredentialsPanel.CredentialsPanelFactory credPanelFactory;
	private final EntityCredentialManagement ecredMan;
	private final NotificationPresenter notificationPresenter;

	EntityCreationHandler(MessageSource msg, EntityManagement identitiesMan,
			CredentialRequirementManagement credReqMan, GroupManagementHelper groupHelper,
			AttributeTypeManagement attrMan,
			IdentityEditorRegistry identityEditorReg, AttributeSupport attributeSupport,
			AttributeHandlerRegistry attributeHandlerRegistry,
			NewEntityCredentialsPanel.CredentialsPanelFactory credPanelFactory, EntityCredentialManagement ecredMan,
			NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.identitiesMan = identitiesMan;
		this.credReqMan = credReqMan;
		this.groupHelper = groupHelper;
		this.attrMan = attrMan;
		this.identityEditorReg = identityEditorReg;
		this.attributeSupport = attributeSupport;
		this.attributeHandlerRegistry = attributeHandlerRegistry;
		this.credPanelFactory = credPanelFactory;
		this.ecredMan = ecredMan;
		this.notificationPresenter = notificationPresenter;
	}

	SingleActionHandler<IdentityEntry> getAction(
			Supplier<Group> initialGroup,
			Consumer<Identity> callback)
	{
		return SingleActionHandler.builder(IdentityEntry.class)
				.withCaption(msg.getMessage("Identities.addEntityAction"))
				.withIcon(VaadinIcon.PLUS_CIRCLE_O)
				.dontRequireTarget()
				.withHandler(selection -> 
					showAddEntityDialog(initialGroup, callback))
				.build();
	}

	void showAddEntityDialog(Supplier<Group> initialGroup, Consumer<Identity> callback)
	{
		new EntityCreationDialog(msg, initialGroup.get(), identitiesMan, 
				credReqMan, attrMan, identityEditorReg, groupHelper, 
				callback, attributeSupport, attributeHandlerRegistry, credPanelFactory, ecredMan, notificationPresenter)
				.open();
	}
}
