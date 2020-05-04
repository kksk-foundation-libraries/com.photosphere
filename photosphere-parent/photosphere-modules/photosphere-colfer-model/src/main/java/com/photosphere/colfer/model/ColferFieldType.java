package com.photosphere.colfer.model;

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public class ColferFieldType {
	private String name;
	private boolean listed;
	private String disp;

	public void calc() {
		disp = (listed ? "[]" : "") + name;
	}

	public ColferFieldType(String name, boolean listed) {
		this.name = name;
		this.listed = listed;
		calc();
	}

	public static final ColferFieldType UINT8 = new ColferFieldType("uint8", false);
	public static final ColferFieldType UINT16 = new ColferFieldType("uint16", false);
	public static final ColferFieldType UINT32 = new ColferFieldType("uint32", false);
	public static final ColferFieldType UINT64 = new ColferFieldType("uint64", false);
	public static final ColferFieldType INT32 = new ColferFieldType("int32", false);
	public static final ColferFieldType INT64 = new ColferFieldType("int64", false);
	public static final ColferFieldType FLOAT32 = new ColferFieldType("float32", false);
	public static final ColferFieldType FLOAT64 = new ColferFieldType("float64", false);
	public static final ColferFieldType TIMESTAMP = new ColferFieldType("timestamp", false);
	public static final ColferFieldType TEXT = new ColferFieldType("text", false);
	public static final ColferFieldType BINARY = new ColferFieldType("binary", false);
	public static final ColferFieldType UINT8_L = new ColferFieldType("uint8", true);
	public static final ColferFieldType UINT16_L = new ColferFieldType("uint16", true);
	public static final ColferFieldType UINT32_L = new ColferFieldType("uint32", true);
	public static final ColferFieldType UINT64_L = new ColferFieldType("uint64", true);
	public static final ColferFieldType INT32_L = new ColferFieldType("int32", true);
	public static final ColferFieldType INT64_L = new ColferFieldType("int64", true);
	public static final ColferFieldType FLOAT32_L = new ColferFieldType("float32", true);
	public static final ColferFieldType FLOAT64_L = new ColferFieldType("float64", true);
	public static final ColferFieldType TEXT_L = new ColferFieldType("text", true);
	public static final ColferFieldType BINARY_L = new ColferFieldType("binary", true);
	public static final ColferFieldType TIMESTAMP_L = new ColferFieldType("timestamp", true);

	public static ColferFieldType of(ColferField field, boolean listed) {
		return new ColferFieldType(field.name(), listed);
	}
}
