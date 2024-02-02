/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.vaadin.endpoint.common.api.services.idp;

import pl.edu.icm.unity.base.group.Group;

/**
 * Helper class for creating mandatory group selection
 *
 * @author P.Piernik
 */
public record GroupWithIndentIndicator(
		Group group,
		boolean indent)
{
}
