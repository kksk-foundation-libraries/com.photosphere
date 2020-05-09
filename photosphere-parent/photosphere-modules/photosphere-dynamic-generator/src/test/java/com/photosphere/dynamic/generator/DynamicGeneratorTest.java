package com.photosphere.dynamic.generator;

import static org.junit.Assert.*;

import java.io.Closeable;
import java.io.IOException;

import org.junit.Test;

import com.photosphere.colfer.generator.CompileException;

public class DynamicGeneratorTest {

	@Test
	public void test() {
		String pkg = DynamicGeneratorTest.class.getPackage().getName();
		DynamicGenerator gen = new DynamicGenerator();
		try {
			for (int i = 0; i < 10; i++) {
				String cls = DynamicGeneratorTest.class.getSimpleName() + "_Object" + i;
				Closeable closeable = gen.newInstance(source(pkg, cls), fqcn(pkg, cls));
				closeable.close();
			}
			for (int i = 0; i < 10; i++) {
				String cls = DynamicGeneratorTest.class.getSimpleName() + "_Object" + i;
				Closeable closeable = gen.newInstance(source(pkg, cls), fqcn(pkg, cls));
				closeable.close();
			}
		} catch (CompileException e) {
			e.printStackTrace();
			fail("Not yet implemented");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String fqcn(String pkg, String cls) {
		return String.join(".", pkg, cls);
	}

	private String source(String pkg, String cls) {
		return String.join("\n" //
				, "package " + pkg + ";" //
				, "" //
				, "public class " + cls + " implements " + Closeable.class.getName() + " {" //
				, "  public void close() throws " + IOException.class.getName() + " {" //
				, "    System.out.println(\"test! " + cls + "\");" //
				, "  }" //
				, "}" //
		);
	}
}
