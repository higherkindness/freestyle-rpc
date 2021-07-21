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

package higherkindness.mu.rpc
package avro

import cats.effect.{IO, Resource}
import io.grpc.ServerServiceDefinition
import higherkindness.mu.rpc.common.{A => _, _}
import higherkindness.mu.rpc.testing.servers.{withServerChannel, ServerChannel}
import org.scalatest._
import shapeless.{:+:, CNil, Coproduct}

class RPCTests extends RpcBaseTestSuite with OneInstancePerTest {

  import higherkindness.mu.rpc.avro.Utils._
  import higherkindness.mu.rpc.avro.Utils.implicits._

  def createClient(
      sc: ServerChannel
  ): Resource[IO, service.RPCService[IO]] =
    service.RPCService.clientFromChannel[IO](IO(sc.channel))

  def runSucceedAssertion[A](ssd: Resource[IO, ServerServiceDefinition], response: A)(
      f: service.RPCService[IO] => IO[A]
  ): Assertion =
    withServerChannel[IO](ssd)
      .flatMap(createClient)
      .use(f)
      .unsafeRunSync() shouldBe response

  def runFailedAssertion[A](
      ssd: Resource[IO, ServerServiceDefinition]
  )(f: service.RPCService[IO] => IO[A]): Assertion =
    withServerChannel[IO](ssd)
      .flatMap(createClient)
      .use(f)
      .attempt
      .unsafeRunSync() shouldBe an[Left[io.grpc.StatusRuntimeException, A]]

  "An AvroWithSchema service with an updated request model" can {

    "add a new non-optional field, and" should {
      "be able to respond to an outdated request without the new value" in {
        runSucceedAssertion(
          serviceRequestAddedBoolean.RPCService.bindService[IO],
          response
        )(_.get(request))
      }
      "be able to respond to an outdated request without the new value within a coproduct" in {
        runSucceedAssertion(
          serviceRequestAddedBoolean.RPCService.bindService[IO],
          responseCoproduct(response)
        )(_.getCoproduct(requestCoproduct(request)))
      }
    }

    "add a new optional field, and" should {
      "be able to respond to an outdated request without the new optional value" in {
        runSucceedAssertion(
          serviceRequestAddedOptionalBoolean.RPCService.bindService[IO],
          response
        )(_.get(request))
      }
      "be able to respond to an outdated request without the new optional value within a coproduct" in {
        runSucceedAssertion(
          serviceRequestAddedOptionalBoolean.RPCService.bindService[IO],
          responseCoproduct(response)
        )(_.getCoproduct(requestCoproduct(request)))
      }
    }

    "add a new item in coproduct, and" should {
      "be able to respond to an outdated request with the previous coproduct" in {
        runSucceedAssertion(
          serviceRequestAddedCoproductItem.RPCService.bindService[IO],
          responseCoproduct(response)
        )(_.getCoproduct(requestCoproduct(request)))
      }
    }

    "remove an item in coproduct, and" should {
      "be able to respond to an outdated request with the previous coproduct" in {
        runSucceedAssertion(
          serviceRequestRemovedCoproductItem.RPCService.bindService[IO],
          responseCoproduct(response)
        )(_.getCoproduct(requestCoproduct(request)))
      }

      "be able to respond to an outdated request with the removed valued of the previous coproduct" ignore {
        runSucceedAssertion(
          serviceRequestRemovedCoproductItem.RPCService.bindService[IO],
          responseCoproduct(response)
        )(_.getCoproduct(requestCoproductInt))
      }

    }

    "replace an item in coproduct, and" should {
      "be able to respond to an outdated request with the previous coproduct" in {
        runSucceedAssertion(
          serviceRequestReplacedCoproductItem.RPCService.bindService[IO],
          responseCoproduct(response)
        )(_.getCoproduct(requestCoproduct(request)))
      }

      "be able to respond to an outdated request with the previous coproduct AAAAA" in {
        runFailedAssertion(
          serviceRequestReplacedCoproductItem.RPCService.bindService[IO]
        )(_.getCoproduct(requestCoproductString))
      }

    }

    "remove an existing field, and" should {
      "be able to respond to an outdated request with the old value" in {
        runSucceedAssertion(
          serviceRequestDroppedField.RPCService.bindService[IO],
          response
        )(_.get(request))
      }
      "be able to respond to an outdated request with the old value within a coproduct" in {
        runSucceedAssertion(
          serviceRequestDroppedField.RPCService.bindService[IO],
          responseCoproduct(response)
        )(_.getCoproduct(requestCoproduct(request)))
      }
    }

    "replace the type of a field, and" should {
      "be able to respond to an outdated request with the previous value" in {
        runSucceedAssertion(
          serviceRequestReplacedType.RPCService.bindService[IO],
          response
        )(_.get(request))
      }
      "be able to respond to an outdated request with the previous value within a coproduct" in {
        runSucceedAssertion(
          serviceRequestReplacedType.RPCService.bindService[IO],
          responseCoproduct(response)
        )(_.getCoproduct(requestCoproduct(request)))
      }
    }

    "rename an existing field, and" should {
      "be able to respond to an outdated request with the previous name" in {
        runSucceedAssertion(
          serviceRequestRenamedField.RPCService.bindService[IO],
          response
        )(_.get(request))
      }
      "be able to respond to an outdated request with the previous name within a coproduct" in {
        runSucceedAssertion(
          serviceRequestRenamedField.RPCService.bindService[IO],
          responseCoproduct(response)
        )(_.getCoproduct(requestCoproduct(request)))
      }
    }

  }

  "An AvroWithSchema service with an updated response model" can {

    "add a new non-optional field, and" should {
      "be able to provide a compatible response" in {
        runSucceedAssertion(
          serviceResponseAddedBoolean.RPCService.bindService[IO],
          response
        )(_.get(request))
      }
      "be able to provide a compatible response within a coproduct" in {
        runSucceedAssertion(
          serviceResponseAddedBoolean.RPCService.bindService[IO],
          responseCoproduct(response)
        )(_.getCoproduct(requestCoproduct(request)))
      }
    }

    "add a new item in a coproduct, and" should {
      "be able to provide a compatible response within a coproduct" in {
        runSucceedAssertion(
          serviceResponseAddedBooleanCoproduct.RPCService.bindService[IO],
          ResponseCoproduct(Coproduct[Int :+: String :+: Response :+: CNil](0))
        )(_.getCoproduct(requestCoproduct(request)))
      }
    }

    "remove an item in a coproduct, and" should {
      "be able to provide a compatible response" in {
        runSucceedAssertion(
          serviceResponseRemovedIntCoproduct.RPCService.bindService[IO],
          responseCoproduct(response)
        )(_.getCoproduct(requestCoproduct(request)))
      }
    }

    "replace an item in a coproduct, and" should {
      "be able to provide a compatible response" in {
        runSucceedAssertion(
          serviceResponseReplacedCoproduct.RPCService.bindService[IO],
          responseCoproduct(response)
        )(_.getCoproduct(requestCoproduct(request)))
      }

    }

    "change the type of a field, and" should {
      "be able to provide a compatible response" in {
        runSucceedAssertion(
          serviceResponseReplacedType.RPCService.bindService[IO],
          response
        )(_.get(request))
      }
      "be able to provide a compatible response within a coproduct" in {
        runSucceedAssertion(
          serviceResponseReplacedType.RPCService.bindService[IO],
          responseCoproduct(response)
        )(_.getCoproduct(requestCoproduct(request)))
      }
    }

    "rename a field, and" should {
      "be able to provide a compatible response" in {
        runSucceedAssertion(
          serviceResponseRenamedField.RPCService.bindService[IO],
          response
        )(_.get(request))
      }
      "be able to provide a compatible response within a coproduct" in {
        runSucceedAssertion(
          serviceResponseRenamedField.RPCService.bindService[IO],
          responseCoproduct(response)
        )(_.getCoproduct(requestCoproduct(request)))
      }
    }

    "drop a field, and" should {
      "be able to provide a compatible response" in {
        runSucceedAssertion(
          serviceResponseDroppedField.RPCService.bindService[IO],
          response
        )(_.get(request))
      }
      "be able to provide a compatible response within a coproduct" in {
        runSucceedAssertion(
          serviceResponseDroppedField.RPCService.bindService[IO],
          responseCoproduct(response)
        )(_.getCoproduct(requestCoproduct(request)))
      }
    }

  }

}
