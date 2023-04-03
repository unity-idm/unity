/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.layout;

import java.util.List;
import java.util.function.Function;

import pl.edu.icm.unity.store.MapperTestBase;
import pl.edu.icm.unity.store.Pair;
import pl.edu.icm.unity.types.registration.layout.BasicFormElement;
import pl.edu.icm.unity.types.registration.layout.FormLayout;
import pl.edu.icm.unity.types.registration.layout.FormLayoutElement;

public class FormLayoutMapperTest extends MapperTestBase<FormLayout, DBFormLayout>

{

	@Override
	protected FormLayout getFullAPIObject()
	{
		return new FormLayout(List.of(new BasicFormElement(FormLayoutElement.REG_CODE)));
	}

	@Override
	protected DBFormLayout getFullDBObject()
	{
		return DBFormLayout.builder()
				.withElements(List.of(DBBasicFormElement.builder()
						.withType("REG_CODE")
						.withFormContentsRelated(true)
						.build()))
				.build();
	}

	@Override
	protected Pair<Function<FormLayout, DBFormLayout>, Function<DBFormLayout, FormLayout>> getMapper()
	{
		return Pair.of(FormLayoutMapper::map, FormLayoutMapper::map);
	}

}
