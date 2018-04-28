package io.npbool.sscustom

import io.npbool.sscustom.local.LocalProxy

object Application extends App {
  new LocalProxy(12345).run()
}
