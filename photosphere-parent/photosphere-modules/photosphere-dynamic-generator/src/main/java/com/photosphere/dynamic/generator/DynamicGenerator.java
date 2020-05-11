package com.photosphere.dynamic.generator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.photosphere.colfer.generator.CompileException;

public class DynamicGenerator {
	private static final Logger LOG = LoggerFactory.getLogger(DynamicGenerator.class);
	public static final DynamicGenerator instance = new DynamicGenerator();
	
	private final ConcurrentMap<String, Constructor<?>> constructors = new ConcurrentHashMap<>();
	private final ClassLoader cl = ClassLoader.getSystemClassLoader();
	private final Method m;
	private final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
	private final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
	private final StandardJavaFileManager standardFileManager = compiler.getStandardFileManager(diagnostics, null, null);
	private final ConcurrentMap<String, JavaFileObject> volatileMap = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, String> aliasMap = new ConcurrentHashMap<>();
	private final JavaFileManager fileManager = new ForwardingJavaFileManager<StandardJavaFileManager>(standardFileManager) {
		@Override
		public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) throws IOException {
			return volatileMap.get(className);
		}
	};

	private DynamicGenerator() {
		Method method = null;
		try {
			method = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, Integer.TYPE, Integer.TYPE);
			method.setAccessible(true);
		} catch (Exception ignore) {
		}
		m = method;
	}
	
	public boolean isLoadedAlias(String alias) {
		return aliasMap.containsKey(alias);
	}
	
	public boolean isLoaded(String fqcn) {
		return constructors.containsKey(fqcn);
	}

	@SuppressWarnings("unchecked")
	public <T> T createInstance(String fqcn) throws CompileException {
		Constructor<?> constructor = constructors.get(fqcn);
		if (constructor == null)
			throw new CompileException(new NullPointerException());
		try {
			return (T) constructor.newInstance();
		} catch (Exception e) {
			throw new CompileException(e);
		}
	}

	public <T> T createInstanceByAlias(String alias) throws CompileException {
		String fqcn = aliasMap.get(alias);
		return createInstance(fqcn);
	}

	public <T> T createInstance(String source, String fqcn, String alias) throws CompileException {
		AtomicReference<CompileException> errors = new AtomicReference<>();
		constructors.computeIfAbsent(fqcn, _fqcn -> {
			try {
				return execute(source, _fqcn, alias);
			} catch (CompileException e) {
				errors.set(e);
				return null;
			}
		});
		aliasMap.putIfAbsent(alias, fqcn);
		if (errors.get() != null) {
			throw errors.get();
		}
		return createInstance(fqcn);
	}

	private Constructor<?> execute(String source, String fqcn, String alias) throws CompileException {
		try {
			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			final SimpleJavaFileObject output = new SimpleJavaFileObject(URI.create("bytes:///" + fqcn.replaceAll("\\.", "/")), Kind.CLASS) {
				ByteArrayOutputStream _baos = baos;

				@Override
				public OutputStream openOutputStream() throws IOException {
					return _baos;
				}
			};
			volatileMap.put(fqcn, output);

			JavaCompiler.CompilationTask task = null;
			task = compiler.getTask(null, fileManager, diagnostics, null, null, Arrays.asList(new SimpleJavaFileObject(URI.create("string:///" + fqcn.replaceAll("\\.", "/") + Kind.SOURCE.extension), Kind.SOURCE) {
				@Override
				public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
					return source;
				}
			}));
			if (!task.call()) {
				diagnostics.getDiagnostics().stream().map(o -> o.toString()).forEach(LOG::info);
			}
			fileManager.flush();
			volatileMap.remove(fqcn);

			byte[] data = baos.toByteArray();
			m.invoke(cl, fqcn, data, 0, data.length);
			Class<?> clazz = cl.loadClass(fqcn);
			return clazz.getConstructor();
		} catch (Exception e) {
			throw new CompileException(e);
		}
	}
}
