package io.github.mcgizzle

sealed case class DecodeFailure(msg: String) extends Exception {
  final override def fillInStackTrace(): Throwable = this
}
