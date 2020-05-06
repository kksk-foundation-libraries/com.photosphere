package com.photosphere.colfer.generator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Generator {
	private static final Logger LOG = LoggerFactory.getLogger(Generator.class);

	public void execute(GenerateConfig generateConfig, File directory, File schema) throws CompileException {
		Process proc = launch(generateConfig, directory, schema);

		Scanner stderr = new Scanner(proc.getErrorStream());
		while (stderr.hasNext())
			LOG.error(stderr.nextLine());

		int exit;
		try {
			exit = proc.waitFor();
			if (exit != 0)
				throw new CompileException("colf: exit " + exit);
		} catch (InterruptedException e) {
			throw new CompileException("interrupted.", e);
		}
	}

	Process launch(GenerateConfig generateConfig, File directory, File schema) throws CompileException {
		List<String> args = new ArrayList<>();
		args.add(compiler(directory).toString());
		args.add("-b=" + generateConfig.sourceTarget());
		if (generateConfig.packagePrefix() != null)
			args.add("-p=" + generateConfig.packagePrefix());
		if (generateConfig.sizeMax() != null)
			args.add("-s=" + generateConfig.sizeMax());
		if (generateConfig.listMax() != null)
			args.add("-l=" + generateConfig.listMax());
		if (generateConfig.superClass() != null)
			args.add("-x=" + generateConfig.superClass());
		if (generateConfig.formatSchemas())
			args.add("-f");
		args.add(generateConfig.lang());
		args.add(schema.toString());

		LOG.info("compile command arguments: " + args);
		ProcessBuilder builder = new ProcessBuilder(args);
		builder.directory(directory);
		try {
			return builder.start();
		} catch (IOException e) {
			throw new CompileException("could not launch", e);
		}
	}

	Path compiler(File directory) throws CompileException {
		String command = "colf";
		String resource;
		{
			String arch = System.getProperty("os.arch").toLowerCase();
			if ("x86_64".equals(arch))
				arch = "amd64";
			if (!"amd64".equals(arch))
				throw new CompileException("unsupported hardware architecture: " + arch);

			String os = System.getProperty("os.name", "generic").toLowerCase();
			if (os.startsWith("mac") || os.startsWith("darwin")) {
				resource = "/" + arch + "/colf-darwin";
			} else if (os.startsWith("windows")) {
				resource = "/" + arch + "/colf.exe";
				command = "colf.exe";
			} else {
				resource = "/" + arch + "/colf-" + os;
			}
		}
		Path path = new File(directory, command).toPath();

		if (Files.exists(path))
			return path;

		// install resource to path
		InputStream stream = Generator.class.getResourceAsStream(resource);
		if (stream == null)
			throw new CompileException(resource + ": no such resource - platform not supported");
		try {
			Files.createDirectories(path.getParent());
			Files.copy(stream, path);
			stream.close();
		} catch (Exception e) {
			LOG.error("compiler command installation", e);
			throw new CompileException(path.toString() + ": installation failed");
		}

		try {
			if (path.getFileSystem().supportedFileAttributeViews().contains("posix")) {
				// ensure execution permission
				Set<PosixFilePermission> perms = Files.getPosixFilePermissions(path);
				if (!perms.contains(PosixFilePermission.OWNER_EXECUTE)) {
					perms.add(PosixFilePermission.OWNER_EXECUTE);
					Files.setPosixFilePermissions(path, perms);
				}
			}
		} catch (Exception e) {
			LOG.warn("compiler executable permission", e);
		}

		return path;
	}
}
