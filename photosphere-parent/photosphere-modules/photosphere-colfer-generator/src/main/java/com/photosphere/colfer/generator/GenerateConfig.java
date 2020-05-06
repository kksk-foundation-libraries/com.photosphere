package com.photosphere.colfer.generator;

import java.io.File;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@Builder
public class GenerateConfig {
	private File sourceTarget;
	private String packagePrefix;
	private String sizeMax;
	private String listMax;
	private String superClass;
	private boolean formatSchemas;
	private String lang;
	private File[] schemas;
}
