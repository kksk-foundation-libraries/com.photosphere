package com.photosphere.colfer.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(fluent = true)
public class ColferField {
	private String name;
	private ColferFieldType fieldType;
}
