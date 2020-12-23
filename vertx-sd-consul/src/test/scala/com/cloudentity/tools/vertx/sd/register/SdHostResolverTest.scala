package com.cloudentity.tools.vertx.sd.register


import com.cloudentity.tools.vertx.sd.register.SdHostResolver._
import org.junit.{Assert, Test}

class SdHostResolverTest {
  @Test
  def shouldSelectFirstAddressHostInMatchingNetworkIfNoPreferredHostOrIp() = {
    val resolveHost = SdHostResolver.resolve(
      List(
        Network("localhost", List(Address("127.0.0.1", "local"))),
        Network("docker0", List(Address("11.10.10.1", "authz"), Address("11.10.10.2", "authz-service")))
      )
    ) _

    Assert.assertEquals(Some("authz"), resolveHost(false, Some("docker0"), None, None))
  }

  @Test
  def shouldSelectFirstAddressIpInMatchingNetworkIfNoPreferredHostOrIp() = {
    val resolveHost = SdHostResolver.resolve(
      List(
        Network("localhost", List(Address("127.0.0.1", "local"))),
        Network("docker0", List(Address("11.10.10.1", "authz"), Address("11.10.10.2", "authz-service")))
      )
    ) _

    Assert.assertEquals(Some("11.10.10.1"), resolveHost(true, Some("docker0"), None, None))
  }

  @Test
  def shouldSelectFirstAddressHostInMatchingNetworkByPreferredHost() = {
    val resolveHost = SdHostResolver.resolve(
      List(
        Network("localhost", List(Address("127.0.0.1", "local"))),
        Network("docker0", List(Address("11.10.10.1", "authz-service"), Address("11.10.10.2", "authz")))
      )
    ) _

    Assert.assertEquals(Some("authz"), resolveHost(false, Some("docker0"), None, Some("authz")))
  }

  @Test
  def shouldSelectFirstAddressHostInMatchingNetworkByPreferredIp() = {
    val resolveHost = SdHostResolver.resolve(
      List(
        Network("localhost", List(Address("127.0.0.1", "local"))),
        Network("docker0", List(Address("11.10.10.1", "authz-service"), Address("11.10.11.2", "authz")))
      )
    ) _

    Assert.assertEquals(Some("authz"), resolveHost(false, Some("docker0"), Some("11.10.11"), None))
  }

  @Test
  def shouldSelectFirstAddressHostInMatchingNetworkByPreferredHostAndIp() = {
    val resolveHost = SdHostResolver.resolve(
      List(
        Network("localhost", List(Address("127.0.0.1", "local"))),
        Network("docker0", List(Address("11.10.10.1", "authz-service"), Address("11.10.11.2", "authz")))
      )
    ) _

    Assert.assertEquals(Some("authz"), resolveHost(false, Some("docker0"), Some("11.10.11"), Some("authz")))
  }

  @Test
  def shouldSelectFirstAddressHostInAllNetworkByPreferredHost() = {
    val resolveHost = SdHostResolver.resolve(
      List(
        Network("localhost", List(Address("127.0.0.1", "local"))),
        Network("docker0", List(Address("11.10.10.1", "authz-service"), Address("11.10.10.2", "authz")))
      )
    ) _

    Assert.assertEquals(Some("authz"), resolveHost(false, None, None, Some("authz")))
  }

  @Test
  def shouldSelectNoneIfNothingConfigured() = {
    val resolveHost = SdHostResolver.resolve(
      List(
        Network("localhost", List(Address("127.0.0.1", "local"))),
        Network("docker0", List(Address("11.10.10.1", "authz-service"), Address("11.10.10.2", "authz")))
      )
    ) _

    Assert.assertEquals(None, resolveHost(false, None, None, None))
  }

}
