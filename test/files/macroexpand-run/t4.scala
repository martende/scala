import scala.reflect.macros.{Context => Ctx}

object Impls {
  def foo(c: Ctx)(x: c.Expr[Int]): c.Expr[Int] = {
    import c.universe._
    val bodyx = Apply(Select(x.tree, newTermName("$plus")), List(Literal(Constant(1))))
    c.Expr[Int](bodyx)
  }
  def smally:Int = 10
  def bar(c: Ctx)(x: c.Expr[Int]): c.Expr[Int] = {
    import c.universe._
    val body = Apply(Select(x.tree, newTermName("$plus")), List(Literal(Constant(2))))
    c.Expr[Int](body)
  }

  def quux(c: Ctx)(x: c.Expr[Int]): c.Expr[Int] = {
    import c.universe._
    val body = Apply(Select(x.tree, newTermName("$plus")), List(Literal(Constant(3))))
    c.Expr[Int](body)
  }
}

object Macros {
  object Shmacros {
    def foo(x: Int): Int = macro Impls.foo
  }
  def bar(x: Int): Int = macro Impls.bar
}

class Macros {
  def quux(x: Int): Int = macro Impls.quux
}

object Test extends App {
  import Macros.Shmacros._
  println(foo(2) + Macros.bar(2) * new Macros().quux(4))
}