/*
 * Copyright 2016-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.facebook.buck.eden;

import com.facebook.buck.util.sha1.Sha1HashCode;
import com.facebook.eden.EdenError;
import com.facebook.eden.EdenService;
import com.facebook.thrift.TException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import java.nio.file.Path;

/**
 * Utility to make requests to the Eden thrift API for an (Eden mount point, Buck project root)
 * pair. The Buck project root must be contained by the Eden mount point.
 */
public final class EdenMount {
  private final EdenService.Client client;

  /** Value of the mountPoint argument to use when communicating with Eden via the Thrift API. */
  private final String mountPoint;

  /** Root of the Buck project of interest that is contained by this {@link EdenMount}. */
  private final Path projectRoot;

  /**
   * Relative path used to resolve paths under the {@link #projectRoot} in the context of the
   * {@link #mountPoint}.
   */
  private final Path prefix;

  /**
   * Creates a new object for communicating with Eden that is bound to the specified
   * (Eden mount point, Buck project root) pair. It must be the case that
   * {@code projectRoot.startsWith(mountPoint)}.
   */
  EdenMount(EdenService.Client client, Path mountPoint, Path projectRoot) {
    Preconditions.checkArgument(
        projectRoot.startsWith(mountPoint),
        "Eden mount point %s must contain the Buck project at %s.",
        mountPoint,
        projectRoot);
    this.client = client;
    this.mountPoint = mountPoint.toString();
    this.projectRoot = projectRoot;
    this.prefix = mountPoint.relativize(projectRoot);
  }

  /** @return The root to the Buck project that this {@link EdenMount} represents. */
  public Path getProjectRoot() {
    return projectRoot;
  }

  @VisibleForTesting
  Path getPrefix() {
    return prefix;
  }

  /**
   * @param entry is a path that is relative to {@link #getProjectRoot()}.
   */
  public Sha1HashCode getSha1(Path entry) throws EdenError, TException {
    byte[] bytes = client.getSHA1(mountPoint, normalizePathArg(entry));
    return Sha1HashCode.fromBytes(bytes);
  }

  /**
   * Returns the path relative to {@link #getProjectRoot()} if {@code path} is contained by
   * {@link #getProjectRoot()}; otherwise, returns {@link Optional#absent()}.
   */
  Optional<Path> getPathRelativeToProjectRoot(Path path) {
    if (path.isAbsolute()) {
      if (path.startsWith(projectRoot)) {
        return Optional.of(projectRoot.relativize(path));
      } else {
        return Optional.absent();
      }
    } else {
      return Optional.of(path);
    }
  }

  /**
   * @param entry is a path that is relative to {@link #getProjectRoot()}.
   * @return a path that is relative to {@link #mountPoint}.
   */
  private String normalizePathArg(Path entry) {
    return prefix.resolve(entry).toString();
  }

  @Override
  public String toString() {
    return String.format("EdenMount{mountPoint=%s, prefix=%s}", mountPoint, prefix);
  }
}
