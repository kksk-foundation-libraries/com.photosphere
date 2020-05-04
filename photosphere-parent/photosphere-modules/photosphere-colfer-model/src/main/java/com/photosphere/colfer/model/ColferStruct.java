package com.photosphere.colfer.model;

import java.util.LinkedList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(fluent = true)
public class ColferStruct {
	private String name;
	private final List<ColferField> fields = new LinkedList<>();
}
