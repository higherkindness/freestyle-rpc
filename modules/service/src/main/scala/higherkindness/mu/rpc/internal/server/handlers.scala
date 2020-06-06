/*
 * Copyright 2017-2020 47 Degrees Open Source <https://www.47deg.com>
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

package higherkindness.mu.rpc.internal.server

import cats.effect.{Effect, IO}
import cats.data.Kleisli
import cats.syntax.apply._
import io.grpc._
import io.grpc.ServerCall.Listener
import io.grpc.stub.ServerCalls
import io.grpc.stub.ServerCalls.UnaryMethod
import io.grpc.stub.StreamObserver
import higherkindness.mu.rpc.protocol.CompressionType
import natchez.{EntryPoint, Span}

object handlers {

  def unary[F[_]: Effect, Req, Res](
      f: Req => F[Res],
      compressionType: CompressionType
  ): ServerCallHandler[Req, Res] =
    ServerCalls.asyncUnaryCall(unaryMethod[F, Req, Res](f, compressionType))

  def tracingUnary[F[_]: Effect, Req, Res](
      f: Req => Kleisli[F, Span[F], Res],
      methodDescriptor: MethodDescriptor[Req, Res],
      entrypoint: EntryPoint[F],
      compressionType: CompressionType
  ): ServerCallHandler[Req, Res] =
    new ServerCallHandler[Req, Res] {
      def startCall(
          call: ServerCall[Req, Res],
          metadata: Metadata
      ): Listener[Req] = {
        val kernel = extractTracingKernel(metadata)
        val spanResource =
          entrypoint.continueOrElseRoot(methodDescriptor.getFullMethodName(), kernel)

        val method = unaryMethod[F, Req, Res](
          req => spanResource.use(span => f(req).run(span)),
          compressionType
        )

        ServerCalls.asyncUnaryCall(method).startCall(call, metadata)
      }
    }

  private def unaryMethod[F[_]: Effect, Req, Res](
      f: Req => F[Res],
      compressionType: CompressionType
  ): UnaryMethod[Req, Res] =
    new UnaryMethod[Req, Res] {
      override def invoke(request: Req, responseObserver: StreamObserver[Res]): Unit = {
        Effect[F]
          .runAsync(addCompression(responseObserver, compressionType) *> f(request)) {
            completeObserver[IO, Res](responseObserver)
          }
          .toIO
          .unsafeRunAsync(_ => ())
      }
    }

}
