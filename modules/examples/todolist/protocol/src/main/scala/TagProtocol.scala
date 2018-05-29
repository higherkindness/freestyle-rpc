/*
 * Copyright 2017-2018 47 Degrees, LLC. <http://www.47deg.com>
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

package examples.todolist
package protocol

import examples.todolist.protocol.MessageId
import freestyle.rpc.protocol._

trait TagProtocol {

  final case class TagRequest(name: String)

  final case class TagMessage(name: String, id: Int)

  final case class TagList(list: List[TagMessage])

  final case class TagResponse(tag: Option[TagMessage])

  @service(Avro)
  trait TagRpcService[F[_]] {

    @rpc
    def reset(empty: Empty.type): F[MessageId]

    @rpc
    def insert(tagRequest: TagRequest): F[TagResponse]

    @rpc
    def retrieve(id: MessageId): F[TagResponse]

    @rpc
    def list(empty: Empty.type): F[TagList]

    @rpc
    def update(tag: TagMessage): F[TagResponse]

    @rpc
    def destroy(id: MessageId): F[MessageId]

  }

}
