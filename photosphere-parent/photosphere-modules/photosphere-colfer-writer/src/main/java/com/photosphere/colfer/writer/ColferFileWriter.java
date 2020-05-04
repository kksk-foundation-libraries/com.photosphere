package com.photosphere.colfer.writer;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.photosphere.colfer.model.ColferFile;

public class ColferFileWriter {
	private static final Logger LOG = LoggerFactory.getLogger(ColferFileWriter.class);

	private final Template template;
	private final Gson gson;

	private static final Function<File, Writer> CONSOLE_OUT = file -> new PrintWriter(System.out);
	private static final Function<File, Writer> FILE_OUT = file -> {
		if (LOG.isDebugEnabled())
			LOG.debug("output to file :{}", file.getAbsolutePath());
		try {
			return new PrintWriter(new BufferedOutputStream(new FileOutputStream(file)));
		} catch (FileNotFoundException e) {
			return new Writer() {
				@Override
				public void write(char[] cbuf, int off, int len) throws IOException {
					throw e;
				}

				@Override
				public void flush() throws IOException {
				}

				@Override
				public void close() throws IOException {
				}
			};
		}
	};
	private static final Function<File, Writer> LOGGING_OUT = file -> {
		StringWriter w = new StringWriter() {
			public void flush() {
				if (LOG.isDebugEnabled())
					LOG.debug(toString());
				getBuffer().setLength(0);
			}

			@Override
			public void close() throws IOException {
				if (LOG.isDebugEnabled())
					LOG.debug(toString());
			}
		};
		return w;
	};

	private final Function<File, Writer> writerFactory;

	public static ColferFileWriter CONSOLE = of(CONSOLE_OUT);
	public static ColferFileWriter FILE = of(FILE_OUT);
	public static ColferFileWriter LOGGING = of(LOGGING_OUT);

	public static ColferFileWriter of(Function<File, Writer> writerFactory) {
		return new ColferFileWriter(writerFactory);
	}

	private ColferFileWriter(Function<File, Writer> writerFactory) {
		Properties p = new Properties();
		p.setProperty(VelocityEngine.RESOURCE_LOADERS, VelocityEngine.RESOURCE_LOADER_CLASS);
		p.setProperty(VelocityEngine.RESOURCE_LOADER + "." + VelocityEngine.RESOURCE_LOADER_CLASS + "." + VelocityEngine.RESOURCE_LOADER_CLASS, ClasspathResourceLoader.class.getName());
		Velocity.init(p);
		template = Velocity.getTemplate("colfer.vm");
		gson = new GsonBuilder() //
				.create() //
		;
		this.writerFactory = writerFactory;
	}

	public void write(String path, ColferFile... files) {
		File directory = new File(path);
		for (ColferFile file : files) {
			_write(directory, file);
		}
	}

	private void _write(File directory, ColferFile file) {
		Writer writer = writerFactory.apply(new File(directory, file.name() + ".colf"));
		try {
			VelocityContext context = new VelocityContext();
			context.put("file", gson.fromJson(gson.toJson(file), Map.class));
			template.merge(context, writer);
			writer.flush();
		} catch (IOException e) {
			LOG.error("io error", e);
		} finally {
			try {
				if (writerFactory != CONSOLE_OUT) {
					writer.close();
				}
			} catch (IOException e) {
				LOG.error("close error", e);
			}
		}
	}
}
