package com.photosphere.colfer.generator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.file.Path;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.photosphere.test.logging.SimpleLogging;

public class GeneratorTest {
	static {
		SimpleLogging.forPackageDebug();
	}

	private static final Logger LOG = LoggerFactory.getLogger(GeneratorTest.class);

	@Test
	public void testCompiler() {
		try {
			Path compiler = new Generator().compiler(new File(System.getProperty("java.io.tmpdir")));
			LOG.debug("path:{}", compiler.toFile().getAbsolutePath());
			assertEquals("wrong colf path", new File(new File(System.getProperty("java.io.tmpdir")), "colf").getAbsolutePath(), compiler.toFile().getAbsolutePath());
			compiler.toFile().delete();
		} catch (CompileException e) {
			LOG.error("compile error", e);
			fail("error raised.");
		}
	}

}
