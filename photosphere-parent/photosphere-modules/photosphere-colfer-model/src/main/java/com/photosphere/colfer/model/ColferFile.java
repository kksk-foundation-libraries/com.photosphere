package com.photosphere.colfer.model;

import java.util.LinkedList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(fluent = true)
public class ColferFile {
	private String name;
	private final List<ColferStruct> structs = new LinkedList<>();
}
