import Impls._

object Test1 {
  trait T1 { type T[U] <: U }
  trait T2 extends T1 { override type T[U] = macro impl[U] }
  object M extends T2
  type T = M.T[String]
}
