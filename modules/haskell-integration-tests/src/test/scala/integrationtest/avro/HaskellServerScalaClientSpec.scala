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

package integrationtest.avro

import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Resource}
import higherkindness.mu.rpc._
import higherkindness.mu.rpc.protocol.Empty
import integrationtest._
import integrationtest.avro.weather.GetForecastResponse.Weather.SUNNY
import integrationtest.avro.weather._
import io.grpc.CallOptions
import org.scalatest.flatspec.AnyFlatSpec

class HaskellServerScalaClientSpec extends AnyFlatSpec with HaskellServerRunningInDocker {

  def serverPort: Int              = Constants.AvroPort
  def serverExecutableName: String = "avro-server"

  val channelFor: ChannelFor = ChannelForAddress("localhost", serverPort)

  val clientResource: Resource[IO, WeatherService[IO]] = WeatherService.client[IO](
    channelFor,
    options = CallOptions.DEFAULT.withCompression("gzip")
  )

  behavior of "Mu-Haskell server and Mu-Scala client communication using Avro"

  it should "work for a trivial unary call" in {
    val response = clientResource
      .use(client => client.ping(Empty))
      .unsafeRunSync()
    assert(response == Empty)
  }

  it should "work for a unary call" in {
    val request = GetForecastRequest("London", 3)
    val expectedResponse = GetForecastResponse(
      last_updated = "2020-03-20T12:00:00Z",
      daily_forecasts = List(SUNNY, SUNNY, SUNNY)
    )
    val response = clientResource
      .use(client => client.getForecast(request))
      .unsafeRunSync()
    assert(response == expectedResponse)
  }

}
