/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef LIBHDFSPP_TOOLS_HDFS_SETREP
#define LIBHDFSPP_TOOLS_HDFS_SETREP

#include <string>

#include <boost/program_options.hpp>

#include "hdfs-tool.h"

namespace hdfs::tools {
/**
 * {@class Setrep} is an {@class HdfsTool} that changes the replication factor
 * of a file at a given path. If the path is a directory, then it recursively
 * changes the replication factor of all files under the directory tree rooted
 * at the given path.
 */
class Setrep : public HdfsTool {
public:
  /**
   * {@inheritdoc}
   */
  Setrep(int argc, char **argv);

  // Abiding to the Rule of 5
  Setrep(const Setrep &) = default;
  Setrep(Setrep &&) = default;
  Setrep &operator=(const Setrep &) = delete;
  Setrep &operator=(Setrep &&) = delete;
  ~Setrep() override = default;

  /**
   * {@inheritdoc}
   */
  [[nodiscard]] std::string GetDescription() const override;

  /**
   * {@inheritdoc}
   */
  [[nodiscard]] bool Do() override;

protected:
  /**
   * {@inheritdoc}
   */
  [[nodiscard]] bool Initialize() override;

  /**
   * {@inheritdoc}
   */
  [[nodiscard]] bool ValidateConstraints() const override;

  /**
   * {@inheritdoc}
   */
  [[nodiscard]] bool HandleHelp() const override;

  /**
   * Handle the path argument that's passed to this tool.
   *
   * @param path The path to the directory for which we need setrep info.
   * @param replication_factor The replication factor to set to given path and
   * its children.
   *
   * @return A boolean indicating the result of this operation.
   */
  [[nodiscard]] virtual bool
  HandlePath(const std::string &path,
             const std::string &replication_factor) const;

private:
  /**
   * A boost data-structure containing the description of positional arguments
   * passed to the command-line.
   */
  po::positional_options_description pos_opt_desc_;
};
} // namespace hdfs::tools
#endif
