/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.active_value_select;

import java.util.List;

interface ValueSelector
{
	List<Integer> getSelectedValueIndices();
}
