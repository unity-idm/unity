/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.identities;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.CredentialRequirementManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.webadmin.utils.GroupManagementHelper;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;

/**
 * Simplifies entity creation dialog instantiation, also provides handy action
 * @author K. Benedyczak
 */
@Component
public class EntityCreationHandler
{
	@Autowired
	private UnityMessageSource msg;
	@Autowired
	private EntityManagement identitiesMan;
	@Autowired
	private CredentialRequirementManagement credReqMan;
	@Autowired
	private GroupManagementHelper groupHelper;
	@Autowired
	private AttributeTypeManagement attrMan;
	@Autowired
	private IdentityEditorRegistry identityEditorReg;

	SingleActionHandler<IdentityEntry> getAction(
			Supplier<String> initialGroup,
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

	public void showAddEntityDialog(Supplier<String> initialGroup, Consumer<Identity> callback)
	{
		new EntityCreationDialog(msg, initialGroup.get(), identitiesMan, 
				credReqMan, attrMan, identityEditorReg, groupHelper, 
				callback).show();
	}
}
