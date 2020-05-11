package com.photosphere.dynamic.generator;

import static org.junit.Assert.fail;

import java.io.Closeable;
import java.io.IOException;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.photosphere.colfer.generator.CompileException;
import com.photosphere.test.logging.SimpleLogging;

public class DynamicGeneratorTest {
	static {
		SimpleLogging.forPackageDebug();
	}
	private static final Logger LOG = LoggerFactory.getLogger(DynamicGeneratorTest.class);

	private static final int CLASSES = 1_000;
	private static final int INSTANCES = 1_000_000;

	@Test
	public void test() {
		String pkg = DynamicGeneratorTest.class.getPackage().getName();
		DynamicGenerator gen = DynamicGenerator.instance;
		try {
			long start = System.currentTimeMillis();
			for (int i = 0; i < CLASSES; i++) {
				String cls = DynamicGeneratorTest.class.getSimpleName() + "_Object" + i;
				String source = source(pkg, cls);
				String fqcn = fqcn(pkg, cls);
				Closeable closeable = gen.createInstance(source, fqcn, cls);
				closeable.close();
			}
			long end = System.currentTimeMillis();
			LOG.debug("elapsed(1st * {}):{}", CLASSES, (end - start));
			start = System.currentTimeMillis();
			for (int j = 0; j < INSTANCES / CLASSES; j++) {
				for (int i = 0; i < CLASSES; i++) {
					String cls = DynamicGeneratorTest.class.getSimpleName() + "_Object" + i;
					Closeable closeable = gen.createInstanceByAlias(cls);
					closeable.close();
				}
			}
			end = System.currentTimeMillis();
			LOG.debug("elapsed(2nd * {} * {}):{}", CLASSES, INSTANCES / CLASSES, (end - start));
		} catch (CompileException e) {
			LOG.error("could not compile", e);
			fail("Not yet implemented");
		} catch (IOException e) {
			LOG.error("could close", e);
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
				, "    // System.out.println(\"test! " + cls + "\");" //
				, "  }" //
				, "}" //
		);
	}
}
