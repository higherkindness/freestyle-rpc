/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freestyle.rpc.internal.service

import io.grpc.{
  MethodDescriptor,
  ServerCallHandler,
  ServerMethodDefinition,
  ServerServiceDefinition
}

class GRPCServiceDefBuilder[Req, Res] {
  def apply(
      name: String,
      calls: (MethodDescriptor[Req, Res], ServerCallHandler[Req, Res])*): ServerServiceDefinition = {
    val builder = io.grpc.ServerServiceDefinition.builder(name)
    calls
      .foldLeft(builder) {
        case (b, (descriptor, call)) =>
          b.addMethod(ServerMethodDefinition.create(descriptor, call))
      }
      .build()
  }
}
