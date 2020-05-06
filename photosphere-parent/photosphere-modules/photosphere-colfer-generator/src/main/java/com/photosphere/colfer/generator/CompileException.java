package com.photosphere.colfer.generator;

public class CompileException extends Exception {

	private static final long serialVersionUID = 3981893404433810489L;

	public CompileException() {
	}

	public CompileException(String message) {
		super(message);
	}

	public CompileException(Throwable cause) {
		super(cause);
	}

	public CompileException(String message, Throwable cause) {
		super(message, cause);
	}

}
