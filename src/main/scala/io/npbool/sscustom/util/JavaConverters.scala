package io.npbool.sscustom.util

import java.lang

object JavaConverters {
  implicit class IntConverter(val v: Int) extends AnyVal {
    def asJava: Integer = new Integer(v)
  }

  implicit class BooleanConverter(val v: Boolean) extends AnyVal {
    def asJava: java.lang.Boolean = new lang.Boolean(v)
  }
}
