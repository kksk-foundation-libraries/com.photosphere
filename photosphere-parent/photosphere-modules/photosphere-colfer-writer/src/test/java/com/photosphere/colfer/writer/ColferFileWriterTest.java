package com.photosphere.colfer.writer;

import org.junit.Test;

import com.photosphere.colfer.model.ColferField;
import com.photosphere.colfer.model.ColferFieldType;
import com.photosphere.colfer.model.ColferFile;
import com.photosphere.colfer.model.ColferStruct;
import com.photosphere.test.logging.SimpleLogging;

public class ColferFileWriterTest {
	static {
		SimpleLogging.forPackageDebug();
	}

	@Test
	public void test() {
		//		StringWriter sWriter = new StringWriter();
		//		ColferFileWriter writer = ColferFileWriter.of(f -> sWriter);
		ColferFileWriter writer = ColferFileWriter.LOGGING;
		ColferFile file01 = new ColferFile().name("File01");
		ColferStruct struct0101 = new ColferStruct().name("Struct01_01");
		ColferStruct struct0102 = new ColferStruct().name("Struct01_02");
		struct0101.fields().add(new ColferField().name("Field01_01_01").fieldType(ColferFieldType.BINARY));
		struct0102.fields().add(new ColferField().name("Field01_02_01").fieldType(ColferFieldType.TEXT_L));
		file01.structs().add(struct0101);
		file01.structs().add(struct0102);
		writer.write("", file01);
		ColferFile file02 = new ColferFile().name("File02");
		ColferStruct struct0201 = new ColferStruct().name("Struct02_01");
		ColferStruct struct0202 = new ColferStruct().name("Struct02_02");
		struct0201.fields().add(new ColferField().name("Field02_01_01").fieldType(ColferFieldType.BINARY));
		struct0202.fields().add(new ColferField().name("Field02_02_01").fieldType(ColferFieldType.TEXT_L));
		file02.structs().add(struct0201);
		file02.structs().add(struct0202);
		writer.write("", file02);
		ColferFileWriter.CONSOLE.write("", file01, file02);
		//		System.err.println(sWriter.toString());
	}

}
