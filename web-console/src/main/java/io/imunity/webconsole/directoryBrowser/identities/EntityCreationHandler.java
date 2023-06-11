/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.directoryBrowser.identities;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.webconsole.directoryBrowser.groupbrowser.GroupManagementHelper;
import io.imunity.webconsole.directoryBrowser.identities.NewEntityCredentialsPanel.CredentialsPanelFactory;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.CredentialRequirementManagement;
import pl.edu.icm.unity.engine.api.EntityCredentialManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeSupport;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistryV8;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistryV8;

/**
 * Simplifies entity creation dialog instantiation, also provides handy action
 * @author K. Benedyczak
 */
@Component
class EntityCreationHandler
{
	@Autowired
	private MessageSource msg;
	@Autowired
	private EntityManagement identitiesMan;
	@Autowired
	private CredentialRequirementManagement credReqMan;
	@Autowired
	private GroupManagementHelper groupHelper;
	@Autowired
	private AttributeTypeManagement attrMan;
	@Autowired
	private IdentityEditorRegistryV8 identityEditorReg;
	@Autowired
	private AttributeSupport attributeSupport;
	@Autowired
	private AttributeHandlerRegistryV8 attributeHandlerRegistry;
	@Autowired
	private CredentialsPanelFactory credPanelFactory;
	@Autowired
	private EntityCredentialManagement ecredMan;
	
	SingleActionHandler<IdentityEntry> getAction(
			Supplier<Group> initialGroup,
			Consumer<Identity> callback)
	{
		return SingleActionHandler.builder(IdentityEntry.class)
				.withCaption(msg.getMessage("Identities.addEntityAction"))
				.withIcon(Images.addEntity.getResource())
				.dontRequireTarget()
				.withHandler(selection -> 
					showAddEntityDialog(initialGroup, callback))
				.build();
	}

	void showAddEntityDialog(Supplier<Group> initialGroup, Consumer<Identity> callback)
	{
		new EntityCreationDialog(msg, initialGroup.get(), identitiesMan, 
				credReqMan, attrMan, identityEditorReg, groupHelper, 
				callback, attributeSupport, attributeHandlerRegistry, credPanelFactory, ecredMan).show();
	}
}
