package com.archguard.api.service;

import java.nio.file.Path;

/** A scan input with its resolved commit and ownership of its directory. */
public record ProjectSource(Path root, String commitSha) { }
